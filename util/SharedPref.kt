package net.rxaa.util

import android.content.SharedPreferences
import net.rxaa.ext.FileExt

object SharedPref {

    @JvmStatic
    fun objToFile(obj: Any, file: SharedPreferences) {
        val editor = file.edit()
        df.getClassFields(obj.javaClass) { f, i ->
            FileExt.catchLog {
                when (f.type) {
                    String::class.java -> {
                        editor.putString(f.name, "" + f.get(obj))
                    }

                    Integer::class.java,
                    Int::class.java -> {
                        editor.putInt(f.name, f.getInt(obj))
                    }

                    java.lang.Long::class.java,
                    Long::class.java -> {
                        editor.putLong(f.name, f.getLong(obj))
                    }
                    java.lang.Float::class.java,
                    Float::class.java -> {
                        editor.putFloat(f.name, f.getFloat(obj))
                    }
                    java.lang.Boolean::class.java,
                    Boolean::class.java -> {
                        editor.putBoolean(f.name, f.getBoolean(obj))
                    }
                }

            }

        }

        editor.commit()
    }

    @JvmStatic
    fun fileToObj(file: SharedPreferences, obj: Any) {
        df.getClassFields(obj.javaClass) { f, i ->
            FileExt.catchLog {
                if (!file.contains(f.name))
                    return@getClassFields

                when (f.type) {
                    String::class.java -> {
                        f.set(obj, file.getString(f.name, ""))
                    }

                    Integer::class.java,
                    Int::class.java -> {
                        f.setInt(obj, file.getInt(f.name, 0))
                    }

                    java.lang.Long::class.java,
                    Long::class.java -> {
                        f.setLong(obj, file.getLong(f.name, 0))
                    }
                    java.lang.Float::class.java,
                    Float::class.java -> {
                        f.setFloat(obj, file.getFloat(f.name, 0f))
                    }
                    java.lang.Boolean::class.java,
                    Boolean::class.java -> {
                        f.setBoolean(obj, file.getBoolean(f.name, false))
                    }
                }
            }

        }

    }
}