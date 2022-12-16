package com.nosari20.connectivitytest

import android.content.Intent
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.tabs.TabLayoutMediator
import com.nosari20.connectivitytest.ui.checklist.CheckListFragment
import com.nosari20.connectivitytest.ui.dialog.AddTestDialogFragment
import com.nosari20.connectivitytest.ui.viewpager.ViewPager2FragmentAdapter
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {


    private val testList = Configuration
    private val activity = this

    private lateinit var addTestDialogFragment: AddTestDialogFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testList.loadManagedConfigurations(applicationContext);
        testList.loadLocalConfigurations(this)

        val adapter = ViewPager2FragmentAdapter(supportFragmentManager, lifecycle)

        val onLongClick = object:  Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {

                val  pos = Integer.parseInt(""+msg.data.get(ConnectivityTestListAdapter.ConnectivityTestViewHolder.KEY_POSITION))

                val localTests = testList.all().get("local")?.toMutableList()
                if (localTests != null) {
                    localTests.removeAt(pos)
                }
                testList.update("local", localTests as ArrayList<ConnectivityTest>)
                testList.saveLocalConfigurations(activity)
                (adapter.getFragment("Custom") as CheckListFragment).removeFromList(pos)

            }
        }


        Configuration.all().get("local")?.let { CheckListFragment(it, onLongClick) }?.let {
            adapter.addFragment(
                it,"Custom")
        }

        Configuration.all().get("managed")?.let { CheckListFragment(it, null) }?.let {
            adapter.addFragment(
                it,"AppConfig")
        }


        Configuration.all().get("google")?.let { CheckListFragment(it, null) }?.let {
            adapter.addFragment(
                it,"Android")
        }

        viewpager.adapter = adapter


        TabLayoutMediator(tabs, viewpager) { tab, position ->
            tab.text = (viewpager.adapter as ViewPager2FragmentAdapter).getPageTitle(position)
        }.attach()


        val addNewConfigHandler = object:  Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                val bundle = msg.data
                val hostname = bundle.getString(AddTestDialogFragment.KEY_HOSTNAME)+""
                val port = bundle.getInt(AddTestDialogFragment.KEY_PORT)
                val ssl = bundle.getBoolean(AddTestDialogFragment.KEY_SSL)

                val localTests = testList.all().get("local")?.toMutableList()
                if (localTests != null) {
                    val test = ConnectivityTest(
                        hostname,
                        port,
                        ssl
                    )
                    localTests.add(test)
                    testList.update("local", localTests as ArrayList<ConnectivityTest>)
                    testList.saveLocalConfigurations(activity)
                    (adapter.getFragment("Custom") as CheckListFragment).addToList(test)
                    Toast.makeText(applicationContext,"New test added.",Toast.LENGTH_SHORT).show()
                }
            }
        }



        // Import config
        val importConfigResultLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->


            if(uri == null) return@registerForActivityResult;

            try {
                contentResolver.openFileDescriptor(uri, "r")?.use {
                    FileInputStream(it.fileDescriptor).use { fis ->
                        var stringBuilder = StringBuilder()
                        val lineList = mutableListOf<String>()
                        fis.bufferedReader().forEachLine { line ->
                            lineList.add(line)
                        }
                        lineList.forEach{stringBuilder.append(it)}

                        try {
                            testList.loadSerializedConfigurations(stringBuilder.toString())
                            Toast.makeText(applicationContext,"Configuration imported successfully",Toast.LENGTH_SHORT).show()
                            Configuration.all().get("local")?.let {
                                (adapter.getFragment("Custom") as CheckListFragment).setList(it)
                            }
                            Toast.makeText(applicationContext,"Config imported successfully.",Toast.LENGTH_SHORT).show()
                            testList.saveLocalConfigurations(activity)
                            addTestDialogFragment.dismiss()
                        }catch (e :NumberFormatException){
                            Toast.makeText(applicationContext,"Bad format.",Toast.LENGTH_SHORT).show()
                        }
                        catch (e :Exception){
                            Toast.makeText(applicationContext,"Unknown error.",Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Toast.makeText(applicationContext,"File not found " + uri.path.toString(),Toast.LENGTH_LONG).show()

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(applicationContext,"IO Exception " + uri.path.toString(),Toast.LENGTH_LONG).show()

            }

        }
        val importConfigHandler = object:  Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                importConfigResultLauncher.launch(arrayOf("text/*"))
            }
        }



        // Export config
        val exportConfigResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->

            if(uri == null) return@registerForActivityResult;

            try {
                contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).use {
                        it.write(testList.serializeConfig().toByteArray())
                        Toast.makeText(applicationContext,"Config saved to " + uri.path.toString(),Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Toast.makeText(applicationContext,"File not found " + uri.path.toString(),Toast.LENGTH_LONG).show()

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(applicationContext,"IO Exception " + uri.path.toString(),Toast.LENGTH_LONG).show()

            }
            if(addTestDialogFragment.isVisible) {
                addTestDialogFragment.dismiss()
            }
        }

        val exportConfigHandler = object:  Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                exportConfigResultLauncher.launch("connectivity-tests-"+ UUID.randomUUID().toString().substring(0, 8)+".txt")
            }
        }



        addTestDialogFragment = AddTestDialogFragment(addNewConfigHandler, importConfigHandler, exportConfigHandler)

        val fab: View = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            addTestDialogFragment.show(supportFragmentManager, "NoticeDialogFragment")
        }
    }

}
