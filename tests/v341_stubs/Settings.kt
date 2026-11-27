package com.abhi.ganasuno.data
object LibraryStore {val options=mutableMapOf<String,Int>();fun option(key:String,default:Int=0)=options[key]?:default}
object Prefs {var normalize=false}
