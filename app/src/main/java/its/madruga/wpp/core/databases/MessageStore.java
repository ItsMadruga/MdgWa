package its.madruga.wpp.core.databases;

public class MessageStore extends DatabaseModel {
    public MessageStore(String dbName, ClassLoader loader) {
        super(dbName, loader);
    }

    public void createTableBlueMessage() {
        String sql = "CREATE TABLE IF NOT EXISTS chatMessages (id INTEGER PRIMARY KEY AUTOINCREMENT, jid TEXT NOT NULL, message_key TEXT, participant_jid TEXT, markread INTEGER DEFAULT 0);";
        writetableDb.execSQL(sql);
    }

    public String getFirstMessage(String jid) {

        var readable = database.getReadableDatabase();
        var cursor = readable.query("chat", new String[]{"user"}, "user=?", new String[]{jid.split("@")[0]}, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            var index = cursor.getColumnIndex("user");
            if (index >= 0) {

            }
        }
        return "";
    }
}