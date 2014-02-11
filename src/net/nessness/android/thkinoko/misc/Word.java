package net.nessness.android.thkinoko.misc;

public class Word {
    public long id;
    public String value;
    public String key;
    public String ref;
    public String cha;
    public String at;
    public int category;

    public Word() {
    }

    /**
     * リストビューセパレータ用
     *
     * @param category
     */
    public Word(int category) {
        this.category = category;
    }

    /**
     * returns primary character id string
     *
     * @return null if {@value cha} is null or empty.
     */
    public String getPrimaryCharacter() {
        if (cha == null || cha.equals("")) {
            return null;
        }

        return cha.split(Constants.RES_OWNER_CHARACTER_SEPARATOR)[0];
    }
}
