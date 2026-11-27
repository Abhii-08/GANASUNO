package com.abhi.ganasuno.data
// Test-only stand-in for Android-backed LibraryStore. Not included in the app source set.
object LibraryStore { data class Listen(val song: Song,val at:Long,val ms:Long) }
