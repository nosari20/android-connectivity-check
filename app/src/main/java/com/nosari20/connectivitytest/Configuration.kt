package com.nosari20.connectivitytest

import android.app.Activity
import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle
import android.os.Parcelable

object Configuration {

    private var checkList =  HashMap<String,ArrayList<ConnectivityTest>>()

    init {
        // Google services
        val googleServices = arrayListOf<ConnectivityTest>();

        // Global
        googleServices.add(ConnectivityTest("accounts.google.com", 443, true))
        googleServices.add(ConnectivityTest("clients1.google.com", 443, true))
        googleServices.add(ConnectivityTest("play.google.com", 443, true))
        googleServices.add(ConnectivityTest("android.googleapis.com", 443, true))
        googleServices.add(ConnectivityTest("android.apis.google.com", 443, true))


        // Push
        googleServices.add(ConnectivityTest("fcm.googleapis.com", 443, true))
        googleServices.add(ConnectivityTest("fcm-xmpp.googleapis.com", 5228, true))
        googleServices.add(ConnectivityTest("fcm-xmpp.googleapis.com", 5229, true))
        googleServices.add(ConnectivityTest("fcm-xmpp.googleapis.com", 5230, true))
        googleServices.add(ConnectivityTest("mtalk.google.com", 5228, true))
        googleServices.add(ConnectivityTest("mtalk.google.com", 5229, true))
        googleServices.add(ConnectivityTest("mtalk.google.com", 5230, true))

        // Static content
        googleServices.add(ConnectivityTest("lh3.ggpht.com", 443, true))
        googleServices.add(ConnectivityTest("lh4.ggpht.com", 443, true))
        googleServices.add(ConnectivityTest("lh5.ggpht.com", 443, true))
        googleServices.add(ConnectivityTest("lh6.ggpht.com", 443, true))
        googleServices.add(ConnectivityTest("lh1.googleusercontent.com", 443, true))
        googleServices.add(ConnectivityTest("lh2.googleusercontent.com", 443, true))
        googleServices.add(ConnectivityTest("lh3.googleusercontent.com", 443, true))
        googleServices.add(ConnectivityTest("lh4.googleusercontent.com", 443, true))
        googleServices.add(ConnectivityTest("lh5.googleusercontent.com", 443, true))
        googleServices.add(ConnectivityTest("lh6.googleusercontent.com", 443, true))

        checkList.put("google",googleServices)



    }

    fun loadManagedConfigurations(context: Context ) {
        // Managed configurations
        val managedChecks = arrayListOf<ConnectivityTest>();
        val myRestrictionsMgr = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val appRestrictions: Bundle = myRestrictionsMgr.applicationRestrictions
        val parcelables: Array<out Parcelable>? = appRestrictions.getParcelableArray("test_list")
        if (parcelables?.isNotEmpty() == true) {
            // iterate parcelables and cast as bundle
            parcelables.map { it as Bundle }.forEach { testBundle ->
                managedChecks.add(
                    ConnectivityTest(
                        testBundle.getString("test_hostname").toString(),
                        testBundle.getInt("test_port"),
                        testBundle.getBoolean("test_ssl"),
                        testBundle.getString("test_certalias").toString()
                    )
                )
            }
        }
        checkList.put("managed",managedChecks)
    }


    fun serializeConfig(): String {
        var configString = ""
        for (check in checkList.get("local")!!){
            configString += (check.host+","+check.port+","+check.ssl+","+check.certAlias+";")
        }
        return configString
    }

    fun deSerializeConfig(configString: String): ArrayList<ConnectivityTest>  {
        var configList = arrayListOf<ConnectivityTest>()

        val testList = configString.split(";")
        if (testList != null) {
            for (testString in testList){
                val test = testString.split(",")

                if (test.size == 4) {
                        configList.add(
                            ConnectivityTest(
                                test[0],
                                Integer.parseInt(test[1]),
                                test[2].equals("true"),
                                test[3]
                            )
                        )
                }
            }
        }

        return configList
    }

    fun loadLocalConfigurations(activity: Activity ) {
        // Local configuration (from preferences)
        val localChecks = arrayListOf<ConnectivityTest>();
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return

        for (test: ConnectivityTest in deSerializeConfig(sharedPref.getString("test_list", "")!!)){
            localChecks.add(
                test
            )
        }

        checkList.put("local",localChecks)
    }


    fun loadSerializedConfigurations(configString: String) {
        var localChecks = checkList.get("local")
        if (localChecks == null) {
            localChecks = arrayListOf<ConnectivityTest>();
        }

        for (test: ConnectivityTest in deSerializeConfig(configString)){
                localChecks.add(
                    test
                )
        }

        checkList.put("local",localChecks)
    }

    fun saveLocalConfigurations(activity: Activity ) {
        // Local configuration (from preferences)
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString("test_list", serializeConfig())
            apply()
        }
    }


    fun all(): Map<String, ArrayList<ConnectivityTest>> {
        return checkList
    }

    fun update(key: String, list: ArrayList<ConnectivityTest>) {
        checkList.put(key,list)
    }




}
