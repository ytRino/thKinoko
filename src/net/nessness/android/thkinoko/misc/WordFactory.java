package net.nessness.android.thkinoko.misc;

import android.database.Cursor;

/**
 *
 */
public class WordFactory {
    public static Word fromCursor(Cursor c, int category) {
        Word w = new Word();
        w.id = c.getLong(c.getColumnIndex(DBHelper.COLS.ID.colName));
        w.value = c.getString(c.getColumnIndex(DBHelper.COLS.VAL.colName));
        w.key = c.getString(c.getColumnIndex(DBHelper.COLS.KEY.colName));
        w.ref = c.getString(c.getColumnIndex(DBHelper.COLS.REF.colName));
        w.at = c.getString(c.getColumnIndex(DBHelper.COLS.AT.colName));
        w.cha = c.getString(c.getColumnIndex(DBHelper.COLS.CHA.colName));
        w.category = category;
        return w;
    }
}
