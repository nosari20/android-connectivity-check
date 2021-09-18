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

        // Global
        googleServices.add(ConnectivityTest("accounts.google.com", 443, true))
        googleServices.add(ConnectivityTest("accounts.google.fr", 443, true))
        googleServices.add(ConnectivityTest("clients1.google.com", 443, true))
        googleServices.add(ConnectivityTest("play.google.com", 443, true))
        googleServices.add(ConnectivityTest("android.googleapis.com", 443, true))

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


        val knoxServices = arrayListOf<ConnectivityTest>();

        // Global
        knoxServices.add(ConnectivityTest("samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("analytics.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("prod-knoxlog.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("account.samsung.com", 443, true))
        knoxServices.add(ConnectivityTest("gslb.secb2b.com", 443, true))

        // Licences US
        knoxServices.add(ConnectivityTest("us-elm.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("us-prod-klm-b2c.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("us-prod-klm.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("usprod-knoxlog.secb2b.com", 443, true))

        // Licences EMEA
        knoxServices.add(ConnectivityTest("eu-elm.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("eu-prod-klm-b2c.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("eu-prod-klm.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("euprod-knoxlog.secb2b.com", 443, true))

        // Licences CN
        knoxServices.add(ConnectivityTest("china-gslb.secb2b.com.cn", 443, true))
        knoxServices.add(ConnectivityTest("china-elm.secb2b.com.cn", 443, true))
        knoxServices.add(ConnectivityTest("china-b2c-klm.secb2b.com.cn", 443, true))
        knoxServices.add(ConnectivityTest("china-prod-klm.secb2b.com.cn", 443, true))

        // Knox service global
        knoxServices.add(ConnectivityTest("knoxservices.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("pinning.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("pinning-02.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("eula.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("umc-cdn.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("me.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("configure.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("custom.samsungknox.com ", 443, true))
        knoxServices.add(ConnectivityTest("kcc-prod-repo.s3.amazonaws.com", 443, true))
        knoxServices.add(ConnectivityTest("klms-dev.s3.amazonaws.com", 443, true))
        knoxServices.add(ConnectivityTest("eu-api.samsungknox.com", 443, true))

        // Knox service US
        knoxServices.add(ConnectivityTest("us-kc-portal.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("us-kc.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("us-kcc.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("us-segd-api.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("us-segp-api.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("us-segm-api.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("us-kme.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("us-kme-api.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("us-kme-api-mssl.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("us-kme-reseller.samsungknox.com", 443, true))

        // Knox service EMEA
        knoxServices.add(ConnectivityTest("eu-kcc.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("eu-kc-portal.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("eu-kc.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("eu-prod-bulk.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("eu-segd-api.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("eu-segp-api.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("eu-segm-api.secb2b.com", 443, true))
        knoxServices.add(ConnectivityTest("eu-kme.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("eu-kme-api.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("eu-kme-api-mssl.samsungknox.com", 443, true))
        knoxServices.add(ConnectivityTest("eu-kme-reseller.samsungknox.com", 443, true))

        // Knox service CN
        knoxServices.add(ConnectivityTest("china-segd-api.secb2b.com.cn", 443, true))
        knoxServices.add(ConnectivityTest("myknoxapk.blob.core.chinacloudapi.cn", 443, true))

        checkList.put("knox",knoxServices)


        val appleServices = arrayListOf<ConnectivityTest>();
        // General
        appleServices.add(ConnectivityTest("albert.apple.com", 443, true))
        appleServices.add(ConnectivityTest("captive.apple.com", 443, true))
        appleServices.add(ConnectivityTest("gs.apple.com", 443, true))
        appleServices.add(ConnectivityTest("humb.apple.com", 443, true))
        appleServices.add(ConnectivityTest("static.ips.apple.com", 443, true))
        appleServices.add(ConnectivityTest("sq-device.apple.com", 443, true))
        appleServices.add(ConnectivityTest("tbsc.apple.com", 443, true))
        appleServices.add(ConnectivityTest("time-ios.apple.com", 123, false))
        appleServices.add(ConnectivityTest("time.apple.com", 123, false))
        appleServices.add(ConnectivityTest("time-macos.apple.com", 443, false))


        // Management
        appleServices.add(ConnectivityTest("api.push.apple.com", 443, true))
        appleServices.add(ConnectivityTest("deviceenrollment.apple.com", 443, true))
        appleServices.add(ConnectivityTest("deviceservices-external.apple.com", 443, true))
        appleServices.add(ConnectivityTest("gdmf.apple.com\n", 443, true))
        appleServices.add(ConnectivityTest("identity.apple.com", 443, true))
        appleServices.add(ConnectivityTest("iprofiles.apple.com", 443, true))
        appleServices.add(ConnectivityTest("mdmenrollment.apple.com", 443, true))
        appleServices.add(ConnectivityTest("setup.icloud.com", 443, true))
        appleServices.add(ConnectivityTest("vpp.itunes.apple.com", 443, true))
        appleServices.add(ConnectivityTest("business.apple.com", 443, true))
        appleServices.add(ConnectivityTest("school.apple.com", 443, true))
        appleServices.add(ConnectivityTest("isu.apple.com", 443, true))
        appleServices.add(ConnectivityTest("ws-ee-maidsvc.icloud.com", 443, true))

        // Update
        appleServices.add(ConnectivityTest("appldnld.apple.com", 80, false))
        appleServices.add(ConnectivityTest("configuration.apple.com", 443, true))
        appleServices.add(ConnectivityTest("gdmf.apple.com", 443, true))
        appleServices.add(ConnectivityTest("gg.apple.com", 443, true))
        appleServices.add(ConnectivityTest("gnf-mdn.apple.com", 443, true))
        appleServices.add(ConnectivityTest("gnf-mr.apple.com", 443, true))
        appleServices.add(ConnectivityTest("gs.apple.com", 443, true))
        appleServices.add(ConnectivityTest("ig.apple.com", 443, true))
        appleServices.add(ConnectivityTest("mesu.apple.com", 443, true))
        appleServices.add(ConnectivityTest("ns.itunes.apple.com", 443, true))
        appleServices.add(ConnectivityTest("oscdn.apple.com", 443, true))
        appleServices.add(ConnectivityTest("osrecovery.apple.com", 443, true))
        appleServices.add(ConnectivityTest("skl.apple.com", 443, true))
        appleServices.add(ConnectivityTest("swcdn.apple.com", 80, false))
        appleServices.add(ConnectivityTest("swdist.apple.com", 443, true))
        appleServices.add(ConnectivityTest("swdownload.apple.com", 443, true))
        appleServices.add(ConnectivityTest("swpost.apple.com", 80, false))
        appleServices.add(ConnectivityTest("swscan.apple.com", 443, true))
        appleServices.add(ConnectivityTest("updates-http.cdn-apple.com", 80, false))
        appleServices.add(ConnectivityTest("updates.cdn-apple.com", 443, true))
        appleServices.add(ConnectivityTest("xp.apple.com", 443, true))

        // App Store
        appleServices.add(ConnectivityTest("itunes.apple.com", 443, true))
        appleServices.add(ConnectivityTest("apps.apple.com", 443, true))
        appleServices.add(ConnectivityTest("mzstatic.com", 443, true))
        appleServices.add(ConnectivityTest("ppq.apple.com", 443, true))

        // Carrier updates
        appleServices.add(ConnectivityTest("appldnld.apple.com", 80, false))
        appleServices.add(ConnectivityTest("appldnld.apple.com.edgesuite.net", 80, false))
        appleServices.add(ConnectivityTest("itunes.com", 80, false))
        appleServices.add(ConnectivityTest("updates-http.cdn-apple.com", 80, false))
        appleServices.add(ConnectivityTest("updates.cdn-apple.com", 443, true))

        // Cache
        appleServices.add(ConnectivityTest("lcdn-registration.apple.com", 443, true))
        appleServices.add(ConnectivityTest("suconfig.apple.com", 80, false))
        appleServices.add(ConnectivityTest("xp-cdn.apple.com", 443, true))
        appleServices.add(ConnectivityTest("lcdn-locator.apple.com", 443, true))
        appleServices.add(ConnectivityTest("serverstatus.apple.com\n", 443, true))

        // DNS
        appleServices.add(ConnectivityTest("doh.dns.apple.com", 443, true))

        // Cert validation
        appleServices.add(ConnectivityTest("certs.apple.com", 80, false))
        appleServices.add(ConnectivityTest("certs.apple.com", 443, true))
        appleServices.add(ConnectivityTest("crl.apple.com", 80, false))

        appleServices.add(ConnectivityTest("crl.entrust.net", 80, false))
        appleServices.add(ConnectivityTest("crl3.digicert.com", 80, false))
        appleServices.add(ConnectivityTest("crl4.digicert.com", 80, false))
        appleServices.add(ConnectivityTest("ocsp.apple.com", 80, false))
        appleServices.add(ConnectivityTest("ocsp.digicert.cn", 80, false))
        appleServices.add(ConnectivityTest("ocsp.digicert.com", 80, false))
        appleServices.add(ConnectivityTest("ocsp.entrust.net", 80, false))
        appleServices.add(ConnectivityTest("ocsp2.apple.com", 80, false))
        appleServices.add(ConnectivityTest("valid.apple.com", 443, true))

        // Apple ID
        appleServices.add(ConnectivityTest("appleid.apple.com", 443, true))
        appleServices.add(ConnectivityTest("appleid.cdn-apple.com\n", 443, true))
        appleServices.add(ConnectivityTest("idmsa.apple.com", 443, true))
        appleServices.add(ConnectivityTest("gsa.apple.com", 443, true))

        checkList.put("apple",appleServices)

        val testServices = arrayListOf<ConnectivityTest>();
        testServices.add(ConnectivityTest("expired.badssl.com", 443, true))
        testServices.add(ConnectivityTest("wrong.host.badssl.com", 443, true))
        testServices.add(ConnectivityTest("self-signed.badssl.com", 443, true))
        testServices.add(ConnectivityTest("untrusted-root.badssl.com", 443, true))
        testServices.add(ConnectivityTest("core.emm.digital", 443, true))
        testServices.add(ConnectivityTest("sha256.badssl.com", 443, true))
        testServices.add(ConnectivityTest("google.com", 443, true))
        checkList.put("tests",testServices)


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
                        testBundle.getInt("test_port"),
                        testBundle.getBoolean("test_ssl")
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