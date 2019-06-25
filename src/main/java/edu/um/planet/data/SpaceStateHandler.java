package edu.um.planet.data;

import edu.um.planet.Universe;
import edu.um.planet.physics.PhysicalObject;
import edu.um.planet.math.Vector3;

import java.awt.*;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;

/**
 * We have noticed that our simulation can sometimes be to slowly, especially if we want to go to certain points in time.
 * In order to solve this problem, we have developed our own file format which is easily parsable and only requires a minimum
 * amount of memory if we want to load a certain point in time, since the file is divided up equally, so finding the right
 * offset is a matter of simple arithmetic.
 */
public class SpaceStateHandler {


    public static void main(String[] args) {
        universeToFileSection("10years_24h_steps_high_precision", (365 * 10));
    }

    private final static File SPACE_FOLDER = new File("res/states");
    private final static Map<String, SpaceFileMeta> cache = new HashMap<>();

    /**
     * The size of the file header.
     */
    private final static int HEADER_SIZE = Long.BYTES + Long.BYTES + Integer.BYTES + Integer.BYTES;
    /**
     * The size in bytes of storing a single physical object and its associated values (e.g. velocity, position, etc...)
     */
    private final static int PHYSICAL_OBJECT_SIZE = 68;
    /**
     * The size of the id in bytes (integer) thus 4 bytes.
     */
    private final static int ID_SIZE = 4;

    public static Map<String, SpaceFileMeta> getCache() {
        return cache;
    }

    /**
     * Creates a new '.space' file by simulating the universe with 24h steps for a certain amount of days.
     * @param fileName The name of the final state file.
     * @param days The amount of days it should simulate.
     */
    public static void universeToFileSection(String fileName, int days) {
        Universe universe = new Universe();
        universe._TIME_DELTA = 10;
        universe._LOOP_ITERATIONS = 6 * 60 * 24;

        try {
            BufferedOutputStream bufferedWriter = new BufferedOutputStream(new FileOutputStream(String.format("res/states/%s.space", fileName)));

            int OBJECT_NAME_SIZE = 0;
            //--- header
            // STRUCTURE [startTime:8|timeDelta:8|bodyCount:4|nameSize:4]
            {
                //name size
                for(PhysicalObject object : universe.getBodies()) {
                    OBJECT_NAME_SIZE = Math.max(object.getName().length(), OBJECT_NAME_SIZE);
                }

                ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
                header.putLong(universe.getStartTime().toEpochMilli());
                header.putLong(universe._TIME_DELTA * universe._LOOP_ITERATIONS * 1000 * 24);
                header.putInt(universe.getBodies().size());
                header.putInt(OBJECT_NAME_SIZE);
                bufferedWriter.write(header.array());
            }

            //--- name lookup
            {
                ByteBuffer nameTable = ByteBuffer.allocate((ID_SIZE + OBJECT_NAME_SIZE) * universe.getBodies().size());
                for(PhysicalObject body : universe.getBodies()) {
                    nameTable.putInt(body.getId());
                    nameTable.put(nameToByteArray(body.getName(), OBJECT_NAME_SIZE));
                }
                bufferedWriter.write(nameTable.array());

            }

            for(int i = 0; i < (days * 24); i++) {
                if(i % 24 == 0) {
                    bufferedWriter.write(universeToFileSection(universe));
                }
                System.out.println(String.format("%.4f%%", ((i / 24D) / days) * 100));
                universe.update();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This returns a universe at a certain time.
     * @param fileName File path to the state file.
     * @param stepsOffset Time offset.
     * @return Universe after the offset.
     */
    public static Universe get(String fileName, int stepsOffset) {

        List<PhysicalObject> objectList = new LinkedList<>();
        Instant universeTime = null;

        try {
            final String filePath = String.format("%s/%s", SPACE_FOLDER.getAbsolutePath(), fileName);

            // --- load meta
            SpaceFileMeta spaceFileMeta = cache.get(fileName);
            if(spaceFileMeta == null) {
                spaceFileMeta = new SpaceFileMeta(filePath);
                spaceFileMeta.loadMeta();
            }

            final long time = spaceFileMeta.getStartTime();
            final long timeOffset = spaceFileMeta.getTimeOffset();
            final int objectsCount = spaceFileMeta.getObjectsCount();

            // --- timeInSeconds the user wants to load
            universeTime = Instant.ofEpochMilli(time + timeOffset * stepsOffset);
            System.out.println("Data@" + universeTime);

            // --- opening the file
            RandomAccessFile stream = new RandomAccessFile(filePath, "r");

            // --- jump meta information
            stream.skipBytes(spaceFileMeta.getMetaSize());

            // --- calculate offset of data
            final long sectionSize = (objectsCount * PHYSICAL_OBJECT_SIZE);
            final long offset = (sectionSize * stepsOffset);

            byte[] objects = new byte[(int) sectionSize];
            assert stream.skipBytes((int) offset) == offset;
            assert stream.read(objects, 0, (int) sectionSize) == sectionSize;

            ByteBuffer objectBuffer = ByteBuffer.wrap(objects);
            while (objectBuffer.hasRemaining()) {
                int id = objectBuffer.getInt();

                //--- name
                String name = spaceFileMeta.getNameLookupTable().get(id);

                //--- radius
                double radius = objectBuffer.getDouble();

                //--- mass
                double mass = objectBuffer.getDouble();

                Vector3 position = new Vector3(objectBuffer.getDouble(), objectBuffer.getDouble(), objectBuffer.getDouble());
                Vector3 velocity = new Vector3(objectBuffer.getDouble(), objectBuffer.getDouble(), objectBuffer.getDouble());

                objectList.add(new PhysicalObject(id, name, Color.WHITE, radius, mass, position, velocity));

            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Universe(objectList, universeTime);

    }

    /**
     * Turns the current state of the universe into a valid file section!
     * @param universe
     * @return
     */
    private static byte[] universeToFileSection(Universe universe) {

        final int bufferSize = (PHYSICAL_OBJECT_SIZE * universe.getBodies().size());
        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

        for(PhysicalObject object : universe.getBodies()) {
            buffer.putInt(object.getId());
            buffer.putDouble(object.getRadius());
            buffer.putDouble(object.getMass());
            buffer.putDouble(object.getPosition().getX());
            buffer.putDouble(object.getPosition().getY());
            buffer.putDouble(object.getPosition().getZ());
            buffer.putDouble(object.getVelocity().getX());
            buffer.putDouble(object.getVelocity().getY());
            buffer.putDouble(object.getVelocity().getZ());
        }

        buffer.flip();
        return buffer.array();

    }

    private static byte[] nameToByteArray(String value, final int size) {
        assert value.getBytes().length <= size;
        ByteBuffer byteBuffer = ByteBuffer.allocate(size).put(value.getBytes());
        byteBuffer.flip();
        return byteBuffer.array();
    }

    public static void populateCache() {
        String[] files = SPACE_FOLDER.list((dir, name) -> name.endsWith(".space"));

        for(String fileName : files) {
            if(!cache.containsKey(fileName)) {
                SpaceFileMeta meta = new SpaceFileMeta(String.format("%s/%s", SPACE_FOLDER.getAbsolutePath(), fileName));
                meta.loadMeta();
                cache.put(fileName, meta);
            }
        }
    }

    public static class SpaceFileMeta {

        private final String filePath;

        private Map<Integer, String> nameLookupTable = new HashMap<>();
        private long startTime;
        private long timeOffset;
        private int objectsCount;
        private int nameSize;
        private int metaSectionSize;

        public SpaceFileMeta(String filePath) {
            this.filePath = filePath;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getTimeOffset() {
            return timeOffset;
        }

        public int getNameSize() {
            return nameSize;
        }

        public int getObjectsCount() {
            return objectsCount;
        }

        public Map<Integer, String> getNameLookupTable() {
            return nameLookupTable;
        }

        public int getMetaSize() {
            return this.metaSectionSize;
        }

        public void loadMeta() {
            try {
                RandomAccessFile stream = new RandomAccessFile(this.filePath, "r");

                //--- header
                {
                    byte[] header = new byte[HEADER_SIZE];
                    this.metaSectionSize += HEADER_SIZE;
                    assert stream.read(header) == HEADER_SIZE;

                    ByteBuffer buffer = ByteBuffer.wrap(header);
                    this.startTime = buffer.getLong();
                    this.timeOffset = buffer.getLong();
                    this.objectsCount = buffer.getInt();
                    this.nameSize = buffer.getInt();
                }

                //--- name lookup
                {
                    final int NAME_LOOKUP_SIZE = ((this.nameSize + SpaceStateHandler.ID_SIZE) * this.objectsCount);
                    this.metaSectionSize += NAME_LOOKUP_SIZE;
                    byte[] nameLookup = new byte[NAME_LOOKUP_SIZE];
                    assert stream.read(nameLookup) == NAME_LOOKUP_SIZE;

                    ByteBuffer buffer = ByteBuffer.wrap(nameLookup);
                    while (buffer.hasRemaining()) {
                        int id = buffer.getInt();
                        byte[] name = new byte[this.nameSize];
                        for(int i = 0; i < name.length; i++) {
                            name[i] = buffer.get();
                        }
                        this.nameLookupTable.put(id, new String(name));
                    }

                    assert nameLookupTable.size() == objectsCount;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

    }

}
