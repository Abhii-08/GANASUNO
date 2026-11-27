package com.abhi.ganasuno.data
object LibraryStore {
    var restored=""
    fun backup()="{\"schema\":1}"
    fun validateBackup(raw:String){require(raw==backup())}
    fun restore(raw:String){validateBackup(raw);restored=raw}
}
