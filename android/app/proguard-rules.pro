# Keep Room-generated database metadata stable through release shrinking.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Gson serializes/deserializes protocol and export models reflectively.
-keepattributes Signature
-keepattributes *Annotation*
