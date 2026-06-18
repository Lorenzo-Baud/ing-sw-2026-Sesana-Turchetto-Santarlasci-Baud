package it.polimi.ingws.gc8.Persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * FA2 — saves and restores match snapshots on disk using plain Java
 * serialization (no JSON, no extra dependencies; consistent with the TCP
 * transport).
 *
 * <p>One file per match ({@code <id>.snapshot}) plus a one-deep rotation
 * ({@code <id>.snapshot.bak}). Writes are crash-safe: the snapshot is written
 * to a temporary file and then atomically renamed onto the final path, so a
 * crash mid-write never corrupts the live snapshot.</p>
 *
 * <p>The save directory is configurable via the {@code MESOS_SAVE_DIR}
 * environment variable, defaulting to {@code ./mesos-saves}.</p>
 */
public class PersistenceManager {

    private static final Logger LOGGER = Logger.getLogger(PersistenceManager.class.getName());

    private static final String SNAPSHOT_SUFFIX = ".snapshot";
    private static final String BAK_SUFFIX = ".snapshot.bak";
    private static final String TMP_SUFFIX = ".snapshot.tmp";
    private static final String ENV_DIR = "MESOS_SAVE_DIR";
    private static final String DEFAULT_DIR = "./mesos-saves";

    private final Path dir;

    /**
     * @param dir directory where snapshots are stored; created if missing
     */
    public PersistenceManager(Path dir) {
        this.dir = dir;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not create save directory " + dir, e);
        }
    }

    /** @return a manager rooted at {@code MESOS_SAVE_DIR} or {@code ./mesos-saves}. */
    public static PersistenceManager fromEnvironment() {
        String configured = System.getenv(ENV_DIR);
        String raw = (configured == null || configured.isBlank()) ? DEFAULT_DIR : configured;
        return new PersistenceManager(Path.of(raw));
    }

    /**
     * Persists a snapshot atomically: write-to-temp then atomic rename, keeping
     * the previous snapshot as a {@code .bak}. Synchronized per manager so two
     * saves of the same match cannot interleave their temp/rename steps.
     *
     * @param snapshot the snapshot to write
     * @throws IOException if writing or renaming fails
     */
    public void save(MatchSnapshot snapshot) throws IOException {
        saveBytes(snapshot.matchId(), toBytes(snapshot));
    }

    /**
     * FA2: serializes a snapshot to a byte array. Callers serialize the model
     * graph while holding the game lock (so it is consistent), then hand the
     * resulting bytes to {@link #saveBytes} for the slow disk write off the lock.
     *
     * @param snapshot the snapshot to serialize
     * @return the serialized bytes
     * @throws IOException if serialization fails
     */
    public static byte[] toBytes(MatchSnapshot snapshot) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(snapshot);
        }
        return bos.toByteArray();
    }

    /**
     * Persists already-serialized snapshot bytes atomically: write-to-temp then
     * atomic rename, keeping the previous snapshot as a {@code .bak}.
     * Synchronized per manager so two saves of the same match cannot interleave.
     *
     * @param matchId the match whose snapshot is being written
     * @param data    the serialized {@link MatchSnapshot} bytes
     * @throws IOException if writing or renaming fails
     */
    public synchronized void saveBytes(String matchId, byte[] data) throws IOException {
        Path finalPath = snapshotPath(matchId);
        Path tmpPath = dir.resolve(fileBase(matchId) + TMP_SUFFIX);
        Path bakPath = dir.resolve(fileBase(matchId) + BAK_SUFFIX);

        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(tmpPath))) {
            out.write(data);
        }

        // Rotate the current snapshot to .bak before swapping in the new one,
        // so a corrupt new write can fall back to the previous good state.
        if (Files.exists(finalPath)) {
            Files.copy(finalPath, bakPath, StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.move(tmpPath, finalPath,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicFailed) {
            // Some filesystems do not support ATOMIC_MOVE; fall back to a plain
            // replace (still far safer than writing the final file directly).
            Files.move(tmpPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Loads every valid snapshot found in the save directory. Corrupt or
     * unreadable files are skipped with a warning rather than aborting startup.
     *
     * @return all successfully deserialized snapshots
     */
    public List<MatchSnapshot> loadAll() {
        List<MatchSnapshot> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> snapshots = files
                    .filter(p -> p.getFileName().toString().endsWith(SNAPSHOT_SUFFIX))
                    .toList();
            for (Path p : snapshots) {
                MatchSnapshot snap = tryLoad(p);
                if (snap != null) {
                    result.add(snap);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not list save directory " + dir, e);
        }
        return result;
    }

    /** Deletes the snapshot and its {@code .bak} for the given match (cleanup on game over). */
    public synchronized void delete(String matchId) {
        try {
            Files.deleteIfExists(snapshotPath(matchId));
            Files.deleteIfExists(dir.resolve(fileBase(matchId) + BAK_SUFFIX));
            Files.deleteIfExists(dir.resolve(fileBase(matchId) + TMP_SUFFIX));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not delete snapshot for match " + matchId, e);
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private MatchSnapshot tryLoad(Path path) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            Object o = ois.readObject();
            if (o instanceof MatchSnapshot snap) {
                return snap;
            }
            LOGGER.warning("Skipping non-snapshot file: " + path);
        } catch (Exception e) {
            // InvalidClassException (model changed), corruption, truncation…
            LOGGER.log(Level.WARNING, "Skipping unreadable snapshot " + path, e);
        }
        return null;
    }

    private Path snapshotPath(String matchId) {
        return dir.resolve(fileBase(matchId) + SNAPSHOT_SUFFIX);
    }

    /**
     * Maps a user-chosen match id to a safe, collision-resistant file base.
     * The real {@code matchId} is stored inside the snapshot, so this mapping
     * only needs to be deterministic and filesystem-safe, not reversible.
     */
    private String fileBase(String matchId) {
        String sanitized = matchId.replaceAll("[^a-zA-Z0-9_-]", "_");
        return sanitized + "-" + Integer.toHexString(matchId.hashCode());
    }
}
