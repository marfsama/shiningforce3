package com.sf3.vdp1.gui;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueModel;
import com.sf3.util.ByteArrayImageInputStream;
import com.sf3.vdp1.model.*;
import org.apache.commons.io.IOUtils;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** (re)-loads the save state. */
public class SaveStateLoader {
    private final ValueModel pathModel;
    private final ValueModel savestate;

    public SaveStateLoader(PresentationModel<Vdp1Model> pm) {
        this.pathModel = pm.getModel(Vdp1Model.PATH);
        this.savestate = pm.getModel(Vdp1Model.SAVESTATE);

        pathModel.addValueChangeListener(evt -> reloadSavestate());
    }

    private void reloadSavestate() {
        if (pathModel.getValue() == null) {
            return;
        }
        try (ImageInputStream stream = getStream((Path) pathModel.getValue())) {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            Header header = new Header(stream);

            Map<String, Chunk> chunks = readChunks(stream);

            VDP1 vdp1 = new VDP1(stream, chunks.get("VDP1"));
            VDP2 vdp2 = new VDP2(stream, chunks.get("VDP2"));
            savestate.setValue(new SaveState(vdp1, vdp2));
            System.out.println("savestate loaded");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Chunk> readChunks(ImageInputStream stream) throws IOException {
        Map<String, Chunk> chunks = new HashMap<>();
        while (stream.getStreamPosition() < stream.length()) {
            Chunk chunk = new Chunk(stream);
            chunks.put(chunk.getMagic(), chunk);
        }
        return chunks;
    }

    private static ImageInputStream getStream(Path path) throws IOException {
        // MemoryCacheImageInputStream
        InputStream stream = Files.newInputStream(path);
        int b1 = stream.read();
        int b2 = stream.read();
        stream.close();
        if (b1 == 0x1f && b2 == 0x8b) {
            // gzipped Stream. deflate whole stream to memory
            try(GZIPInputStream input = new GZIPInputStream(Files.newInputStream(path))) {
                byte[] content = IOUtils.toByteArray(input);
                return new ByteArrayImageInputStream(content);
            }
        }
        // not gzipped
        return new MemoryCacheImageInputStream(Files.newInputStream(path));
    }

}
