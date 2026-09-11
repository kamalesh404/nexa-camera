package com.nexa.camera

object NativeBridge {
    init { System.loadLibrary("nexa_native") }
    external fun nativeBuildInfo(): String
}
