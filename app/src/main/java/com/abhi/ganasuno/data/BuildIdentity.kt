package com.abhi.ganasuno.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** Detects signing-key mismatches against independently obtained trusted fingerprints.
 * Does not assert that matching signatures prove an unmodified source tree. */
object BuildIdentity {
    private fun prefs(c:Context)=c.getSharedPreferences("build_identity_v28",Context.MODE_PRIVATE)
    fun actual(c:Context):Set<String>{val pm=c.packageManager;val signatures=if(Build.VERSION.SDK_INT>=28){pm.getPackageInfo(c.packageName,PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners.orEmpty()}else{@Suppress("DEPRECATION") val p=pm.getPackageInfo(c.packageName,PackageManager.GET_SIGNATURES);@Suppress("DEPRECATION") val sig=p.signatures;sig.orEmpty()};return signatures.map{s->MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(""){"%02x".format(it.toInt() and 255)}}.toSet()}
    fun setExpected(c:Context,raw:String){val values=Batch28.fingerprints(raw);require(values.isNotEmpty()&&values.size<=8){"Enter valid SHA-256 fingerprints"};prefs(c).edit().putStringSet("expected",values).apply()}
    fun clear(c:Context){prefs(c).edit().remove("expected").apply()}
    fun report(c:Context):String{val a=actual(c);val expected=prefs(c).getStringSet("expected",emptySet()).orEmpty();return Batch28.signatureStatus(a,expected)+"\n\nInstalled certificate SHA-256:\n"+a.joinToString("\n")+"\n\nConfigured trusted SHA-256:\n"+expected.joinToString("\n").ifBlank{"None"}+"\n\nMatching a certificate is not proof that the source code is unchanged. Obtain fingerprints independently."}
}
