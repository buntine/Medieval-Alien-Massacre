
; compression.clj
; Handles all Base64 compression/decompression of strings.
; Thanks to Chris McClellen for this code.
; Source: http://www.mail-archive.com/clojure@googlegroups.com/msg19378.html

(ns mam.compression
  (:use [clojure.contrib.duck-streams :only (to-byte-array)]))


(defn str-to-bytes [s]
  (.getBytes s))

(defn str-from-bytes [b]
  (new String b))

; Encode a raw array of bytes as a base64 array of bytes
(defn encode64 [b]
  (. (new sun.misc.BASE64Encoder) encode b))

; Decode a string encoded in base 64, result as array of bytes
(defn decode64 [s]
  (let [decoder (new sun.misc.BASE64Decoder)]
    (. decoder decodeBuffer s)))

; Compress human readable string and return it as base64 encoded
(defn compress [s]
  (let [b (str-to-bytes s)
        output (new java.io.ByteArrayOutputStream)
        deflater (new java.util.zip.DeflaterOutputStream
                      output
                      (new java.util.zip.Deflater) 1024)]
    (. deflater write b)
    (. deflater close)
    (str-from-bytes (encode64 (. output toByteArray)))))

; Take a string that was compressed & base64 encoded.. and undo all
; that
(defn decompress [s]
  (let [b (decode64 s)
        input (new java.io.ByteArrayInputStream b)
        inflater (new java.util.zip.InflaterInputStream
                      input
                      (new java.util.zip.Inflater) 1024)
        result (to-byte-array inflater)]
    (. inflater close)
    (str-from-bytes result)))
