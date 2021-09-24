package com.nosari20.connectivitytest.ui.checklist


import android.os.Bundle
import android.os.Handler
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
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*


class CheckListFragment(private var list: List<ConnectivityTest>, private val onLongClick: Handler?) : Fragment() {

    companion object {
        val KEY_ID: String = "item_id"
    }

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



        try {

            val client: SSLSocket = SSLSocketFactory.getDefault().run {
                createSocket(test.host, test.port) as SSLSocket
            }

            getActivity()?.runOnUiThread(java.lang.Runnable {
                testlist.adapter?.notifyDataSetChanged()
            })

            val before = System.currentTimeMillis()
            client.startHandshake()

            val after = System.currentTimeMillis()

            val session = client.session

            val verifier = HostnameVerifier { hostname, session ->
                var match = false
                try {

                    val subject = session.peerCertificateChain[0].subjectDN.toString()

                    val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
                    val bais = ByteArrayInputStream(session.peerCertificateChain[0].getEncoded())
                    val x509 = cf.generateCertificate(bais) as X509Certificate
                    val SANs = x509.subjectAlternativeNames

                    for (SAN in SANs) {
                        if(SAN[0] == 2) {// if DNS name
                            val DNSName = SAN[1].toString()
                            match = DNSName.equals(hostname) || // DNSName = example.com, Hostname = example.com
                                    (DNSName.replace(hostname,"").equals(".*")) || // DNSName = *.example.com, Hostname = example.com
                                    (hostname.removeSuffix(DNSName.removePrefix("*.")).endsWith(".")) // DNSName = *.example.com, Hostname = foo.example.com

                            if (match)
                                break
                        }
                    }

                    match

                } catch (e: Exception){
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
                test.info = "Time: " + (after-before) + "ms (" + client.inetAddress.hostAddress+")"+"" +
                        "\n Certificate : OK, Protocol : " +session.protocol
            }

            client.close()

        } catch (e: Exception) {
            test.status = ConnectivityTest.Status.KO
            test.info = e.localizedMessage.removePrefix("java.security.cert.CertPathValidatorException: ")
        } finally {
            getActivity()?.runOnUiThread(java.lang.Runnable {
                testlist.adapter?.notifyDataSetChanged()
            })
        }
    }

}