package com.nosari20.connectivitytest.ui.checklist


import android.os.Bundle
import android.os.Handler
import android.security.KeyChain
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nosari20.connectivitytest.ConnectivityTest
import com.nosari20.connectivitytest.ConnectivityTestListAdapter
import com.nosari20.connectivitytest.R
import java.io.ByteArrayInputStream
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*


class CheckListFragment(private var list: List<ConnectivityTest>, private val onLongClick: Handler?) : Fragment() {

    private lateinit var testlist: RecyclerView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)

        val root: View = inflater.inflate(R.layout.fragment_checklist, container, false)

        testlist = root.findViewById<RecyclerView>(R.id.test_list)
        testlist.apply {
            layoutManager = LinearLayoutManager(this.context)
            adapter = ConnectivityTestListAdapter(list,onLongClick)
        }


        val swipeLayout =  root.findViewById<SwipeRefreshLayout>(R.id.swipe_layout)

        swipeLayout.setOnRefreshListener {
            val thread = Thread {
                for (test: ConnectivityTest in list) {
                    test.status = ConnectivityTest.Status.PENDING
                    val sub_thread = Thread {
                        if(test.ssl){
                            check_ssl(test)
                        } else {
                            check_tcp(test)
                        }

                    }
                    sub_thread.start()
                }

            }
            swipeLayout.setRefreshing(false);
            thread.start()
        }

        return root
    }

    fun addToList(test: ConnectivityTest) {
        val nlist = list.toMutableList()
        nlist.add(test)
        list = nlist

        testlist.apply {
            layoutManager = LinearLayoutManager(this.context)
            adapter = ConnectivityTestListAdapter(list, onLongClick)
        }
    }

    fun removeFromList(pos: Int) {
        val nlist = list.toMutableList()
        nlist.removeAt(pos)
        list = nlist
        testlist.apply {
            layoutManager = LinearLayoutManager(this.context)
            adapter = ConnectivityTestListAdapter(list, onLongClick)
        }
    }

    fun setList(list: ArrayList<ConnectivityTest>) {
        this.list = list
        testlist.apply {
            layoutManager = LinearLayoutManager(this.context)
            adapter = ConnectivityTestListAdapter(list, onLongClick)
        }
    }


    fun check_tcp(test: ConnectivityTest) {
        try {
            getActivity()?.runOnUiThread(java.lang.Runnable {
                testlist.adapter?.notifyDataSetChanged()
            })
            val before = System.currentTimeMillis()
            val client = Socket(test.host, test.port)
            client.close()
            val after = System.currentTimeMillis()
            test.status = ConnectivityTest.Status.OK
            test.info = "Time: " + (after-before) + "ms (" + client.inetAddress.hostAddress+")"
        } catch (e: Exception) {
            test.status = ConnectivityTest.Status.KO
            test.info = e.localizedMessage
        } finally {
            getActivity()?.runOnUiThread(java.lang.Runnable {
                testlist.adapter?.notifyDataSetChanged()
            })
        }
    }

    fun check_ssl(test: ConnectivityTest) {

        var sslFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory

        val clientAuth = !(test.certAlias.isEmpty() || test.certAlias.equals("null"))

        if(clientAuth) {

            val priv = KeyChain.getPrivateKey(this.requireContext(), test.certAlias)
            val pub = KeyChain.getCertificateChain(this.requireContext(), test.certAlias)

            if (priv == null) {
                requestAliasPermission(test);
                return
            }

            val km = object : X509KeyManager {

                override fun getCertificateChain(alias: String?): Array<X509Certificate> {
                    return pub!!
                }

                override fun getPrivateKey(alias: String?): PrivateKey {
                    return priv!!
                }

                override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket ): String {
                    return test.certAlias
                }

                override fun getClientAliases(
                    keyType: String?,
                    issuers: Array<out Principal>?
                ): Array<String> {
                    TODO("Not yet implemented")
                }

                override fun getServerAliases(
                    keyType: String?,
                    issuers: Array<out Principal>?
                ): Array<String> {
                    TODO("Not yet implemented")
                }

                override fun chooseServerAlias(
                    keyType: String?,
                    issuers: Array<out Principal>?,
                    socket: Socket?
                ): String {
                    TODO("Not yet implemented")
                }

            }

            var sslContext = SSLContext.getInstance("TLS")

            sslContext.init(arrayOf<KeyManager>(km), null, null)

            sslFactory = sslContext.socketFactory
        }


        var client: SSLSocket? = null

        try {

            client = sslFactory.run {
                createSocket(test.host, test.port, ) as SSLSocket
            }


            val before = System.currentTimeMillis()
            client.startHandshake()
            val after = System.currentTimeMillis()

            val session = client.session

            val verifier = HostnameVerifier { hostname, session ->
                var match = false
                try {

                    val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
                    val bais = ByteArrayInputStream(session.peerCertificateChain[0].getEncoded())
                    val x509 = cf.generateCertificate(bais) as X509Certificate
                    val SANs = x509.subjectAlternativeNames

                    for (SAN in SANs) {
                        if (SAN[0] == 2) {// if DNS name
                            val DNSName = SAN[1].toString()
                            match =
                                DNSName.equals(hostname) || // DNSName = example.com, Hostname = example.com
                                        (DNSName.replace(hostname, "")
                                            .equals(".*")) || // DNSName = *.example.com, Hostname = example.com
                                        (hostname.removeSuffix(DNSName.removePrefix("*."))
                                            .endsWith(".")) // DNSName = *.example.com, Hostname = foo.example.com

                            if (match)
                                break
                        }
                    }

                    match

                } catch (e: Exception) {
                    match
                } finally {
                    match
                }

            }

            if (!verifier.verify(session.getPeerHost(), session)) {
                test.status = ConnectivityTest.Status.KO
                test.info = "Wrong host"
            } else {
                test.status = ConnectivityTest.Status.OK
                test.info =
                    "Time: " + (after - before) + "ms (" + client.inetAddress.hostAddress + ")" + "" +
                            "\nCertificate : OK, Protocol : " + session.protocol + ( if(clientAuth) " (mutual)" else "")
            }

            client.close()

        } catch (e: SSLHandshakeException) {
            test.status = ConnectivityTest.Status.KO
            test.info = "Error during handshake: "+e.localizedMessage

        } catch (e: Exception) {
            test.status = ConnectivityTest.Status.KO
            test.info = ""+e.localizedMessage

        } finally {
            getActivity()?.runOnUiThread(java.lang.Runnable {
                testlist.adapter?.notifyDataSetChanged()
            })
        }
    }

    fun requestAliasPermission(test: ConnectivityTest){
        KeyChain.choosePrivateKeyAlias(this.requireActivity(),
            { alias ->
                if (alias == null) {
                    test.status = ConnectivityTest.Status.UNKNOWN
                    test.info = "Certificate not selected"
                }else{

                    if(alias != test.certAlias){
                        test.status = ConnectivityTest.Status.UNKNOWN
                        test.info = "Permission granted to wrong certificate"
                    }else{
                        check_ssl(test)
                    }
                }

                getActivity()?.runOnUiThread(java.lang.Runnable {
                    testlist.adapter?.notifyDataSetChanged()
                })


            },  /* keyTypes[] */null,  /* issuers[] */null,  /* uri */null,  /* alias */test.certAlias
        )
    }
}