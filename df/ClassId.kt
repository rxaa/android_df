package rxaa.df

object ClassId {

    val viewMap = HashMap<Class<*>, Int>();

    var classId = 0;

    fun getId(c: Class<*>): Int {
        return viewMap.getOrPut(c) {
            ++classId;
        }
    }
}