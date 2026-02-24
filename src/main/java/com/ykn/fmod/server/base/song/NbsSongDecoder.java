/**
 * Copyright (c) koca2000, Meteor Development, Meteor Client
 * This file is under the GPL-3.0 License
 */

package com.ykn.fmod.server.base.song;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.block.enums.Instrument;

/**
 * The {@code NbsSongDecoder} class is responsible for decoding Note Block Studio (NBS) song files
 * from an input stream and converting them into a {@link NoteBlockSong} object.
 * 
 * <p>Usage:
 * <pre>
 * InputStream inputStream = ...; // Obtain an input stream for the NBS file
 * NoteBlockSong song = NbsSongDecoder.parse(inputStream);
 * </pre>
 */
public class NbsSongDecoder {

    public static final int NOTE_OFFSET = 33;

    public static NoteBlockSong parse(InputStream inputStream) throws IOException {
        HashMap<Double, List<NoteBlockNote>> notesMap = new HashMap<>();
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        short length = readShort(dataInputStream);
        int nbsversion = 0;
        if (length == 0) {
            nbsversion = dataInputStream.readByte();
            dataInputStream.readByte(); // Custom Instrument
            if (nbsversion >= 3) {
                length = readShort(dataInputStream);
            }
        }
        readShort(dataInputStream); // Song Height
        String title = readString(dataInputStream); // Song Name
        String author = readString(dataInputStream); // Song Author
        readString(dataInputStream); // original author
        readString(dataInputStream); // description
        double speed = readShort(dataInputStream) / 100.0; // tempo
        dataInputStream.readBoolean(); // auto-save
        dataInputStream.readByte(); // auto-save duration
        dataInputStream.readByte(); // x/4ths, time signature
        readInt(dataInputStream); // minutes spent on project
        readInt(dataInputStream); // left clicks (why?)
        readInt(dataInputStream); // right clicks (why?)
        readInt(dataInputStream); // blocks added
        readInt(dataInputStream); // blocks removed
        readString(dataInputStream); // .mid/.schematic file name
        if (nbsversion >= 4) {
            dataInputStream.readByte(); // loop on/off
            dataInputStream.readByte(); // max loop count
            readShort(dataInputStream); // loop start tick
        }

        double tick = -1;
        while (true) {
            short jumpTicks = readShort(dataInputStream); // jumps till next tick
            if (jumpTicks == 0) {
                break;
            }
            tick += jumpTicks * (20.0 / speed);
            while (true) {
                short jumpLayers = readShort(dataInputStream); // jumps till next layer
                if (jumpLayers == 0) {
                    break;
                }
                byte instrument = dataInputStream.readByte();

                byte key = dataInputStream.readByte();
                if (nbsversion >= 4) {
                    dataInputStream.readUnsignedByte(); // note block velocity
                    dataInputStream.readUnsignedByte(); // note panning, 0 is right in nbs format
                    readShort(dataInputStream); // note block pitch
                }

                Instrument inst = fromNBSInstrument(instrument);

                // Ignore custom instruments
                if (inst == null) {
                    continue;
                }

                NoteBlockNote note = new NoteBlockNote(inst, key - NOTE_OFFSET);
                setNote(tick, note, notesMap);
            }
        }

        return new NoteBlockSong(notesMap, title, author);
    }

    private static void setNote(double ticks, NoteBlockNote note, HashMap<Double, List<NoteBlockNote>> notesMap) {
        notesMap.computeIfAbsent(ticks, k -> new ArrayList<>()).add(note);
    }

    private static short readShort(DataInputStream dataInputStream) throws IOException {
        int byte1 = dataInputStream.readUnsignedByte();
        int byte2 = dataInputStream.readUnsignedByte();
        return (short) (byte1 + (byte2 << 8));
    }

    private static int readInt(DataInputStream dataInputStream) throws IOException {
        int byte1 = dataInputStream.readUnsignedByte();
        int byte2 = dataInputStream.readUnsignedByte();
        int byte3 = dataInputStream.readUnsignedByte();
        int byte4 = dataInputStream.readUnsignedByte();
        return (byte1 + (byte2 << 8) + (byte3 << 16) + (byte4 << 24));
    }

    private static String readString(DataInputStream dataInputStream) throws IOException {
        int length = readInt(dataInputStream);
        if (length < 0) {
            throw new EOFException("Length should not be negative, but got " + length);
        }
        if (length > dataInputStream.available()) {
            throw new EOFException("Length is larger than a buffer. Available buffer bytes: " + dataInputStream.available() + ", length: " + length);
        }

        StringBuilder builder = new StringBuilder(length);
        while (length > 0) {
            char c = (char) dataInputStream.readByte();
            if (c == (char) 0x0D) {
                c = ' ';
            }
            builder.append(c);
            length--;
        }
        return builder.toString();
    }

    private static Instrument fromNBSInstrument(int instrument) {
        return switch (instrument) {
            case 0 -> Instrument.HARP;
            case 1 -> Instrument.BASS;
            case 2 -> Instrument.BASEDRUM;
            case 3 -> Instrument.SNARE;
            case 4 -> Instrument.HAT;
            case 5 -> Instrument.GUITAR;
            case 6 -> Instrument.FLUTE;
            case 7 -> Instrument.BELL;
            case 8 -> Instrument.CHIME;
            case 9 -> Instrument.XYLOPHONE;
            case 10 -> Instrument.IRON_XYLOPHONE;
            case 11 -> Instrument.COW_BELL;
            case 12 -> Instrument.DIDGERIDOO;
            case 13 -> Instrument.BIT;
            case 14 -> Instrument.BANJO;
            case 15 -> Instrument.PLING;
            default -> null;
        };
    }
}
