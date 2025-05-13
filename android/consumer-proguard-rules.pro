# Uyarı bastırmaları (gereksiz warning'leri engeller)
-dontwarn com.akuple.web_print.async.AsyncBluetoothEscPosPrint
-dontwarn com.akuple.web_print.async.AsyncEscPosPrinter
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn com.gemalto.jp2.JP2Encoder

# Sınıfları koruma (Proguard tarafından silinmemesini sağlar)
-keep class com.akuple.web_print.** { *; }
-keep class com.gemalto.jp2.** { *; }