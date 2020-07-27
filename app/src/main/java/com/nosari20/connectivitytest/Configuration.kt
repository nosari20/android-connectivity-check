package com.nosari20.connectivitytest

import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle
import android.os.Parcelable

object Configuration {

    private var checkList =  HashMap<String,ArrayList<ConnectivityTest>>()
    //get() = this.checkList

    init {
        // Google services
        val googleServices = arrayListOf<ConnectivityTest>();
        googleServices.add(ConnectivityTest("accounts.google.com", 443))
        googleServices.add(ConnectivityTest("accounts.google.fr", 443))
        googleServices.add(ConnectivityTest("clients1.google.com", 443))
        googleServices.add(ConnectivityTest("play.google.com", 443))
        googleServices.add(ConnectivityTest("android.googleapis.com", 443))
        googleServices.add(ConnectivityTest("fcm.googleapis.com", 443))
        googleServices.add(ConnectivityTest("fcm-xmpp.googleapis.com", 5228))
        googleServices.add(ConnectivityTest("fcm-xmpp.googleapis.com", 5229))
        googleServices.add(ConnectivityTest("fcm-xmpp.googleapis.com", 5230))
        googleServices.add(ConnectivityTest("mtalk.google.com", 5228))
        googleServices.add(ConnectivityTest("mtalk.google.com", 5229))
        googleServices.add(ConnectivityTest("mtalk.google.com", 5230))
        googleServices.add(ConnectivityTest("lh3.ggpht.com", 443))
        googleServices.add(ConnectivityTest("lh4.ggpht.com", 443))
        googleServices.add(ConnectivityTest("lh5.ggpht.com", 443))
        googleServices.add(ConnectivityTest("lh6.ggpht.com", 443))
        googleServices.add(ConnectivityTest("lh1.googleusercontent.com", 443))
        googleServices.add(ConnectivityTest("lh2.googleusercontent.com", 443))
        googleServices.add(ConnectivityTest("lh3.googleusercontent.com", 443))
        googleServices.add(ConnectivityTest("lh4.googleusercontent.com", 443))
        googleServices.add(ConnectivityTest("lh5.googleusercontent.com", 443))
        googleServices.add(ConnectivityTest("lh6.googleusercontent.com", 443))

        checkList.put("google",googleServices)

    }

    fun loadManagedConfigurations(context: Context ) {
        // Managed configurations
        val customChecks = arrayListOf<ConnectivityTest>();
        val myRestrictionsMgr = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val appRestrictions: Bundle = myRestrictionsMgr.applicationRestrictions
        val parcelables: Array<out Parcelable>? = appRestrictions.getParcelableArray("test_list")
        if (parcelables?.isNotEmpty() == true) {
            // iterate parcelables and cast as bundle
            parcelables.map { it as Bundle }.forEach { testBundle ->
                // parse bundle data and store in VpnConfig array
                customChecks.add(
                    ConnectivityTest(
                        testBundle.getString("test_hostname").toString(),
                        testBundle.getInt("test_port")
                    )
                )
            }
        }
        checkList.put("custom",customChecks)

    }


    fun all(): Map<String, List<ConnectivityTest>> {
        return checkList
    }







}