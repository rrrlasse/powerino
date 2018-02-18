package com.powerino.prototype;
/**
 * Created by me on 01/02/2018.
 */


public class EEPROM {
    static final short ENTRY_SIZE = 24;

    static final short
            INITIALIZED[] =     { 0 + 0 * ENTRY_SIZE,  0 + 1 * ENTRY_SIZE},
            FORWARDS[] =        { 4 + 0 * ENTRY_SIZE,  4 + 1 * ENTRY_SIZE},
            BACKWARDS[] =       { 8 + 0 * ENTRY_SIZE,  8 + 1 * ENTRY_SIZE},
            DVDF[] =            {12 + 0 * ENTRY_SIZE, 12 + 1 * ENTRY_SIZE},
            ARM[] =             {16 + 0 * ENTRY_SIZE, 16 + 1 * ENTRY_SIZE},
            VELOCITY[] =        {20 + 0 * ENTRY_SIZE, 20 + 1 * ENTRY_SIZE};


}
