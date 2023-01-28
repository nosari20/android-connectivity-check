package com.nosari20.connectivitytest.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.security.KeyChain
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.nosari20.connectivitytest.R


class AddTestDialogFragment(val add_handler: Handler,val import_handler: Handler, val export_handler: Handler) : DialogFragment() {

    companion object {
        val KEY_HOSTNAME: String = "hostname"
        val KEY_PORT: String = "port"
        val KEY_SSL: String = "ssl"
        val KEY_CERTALIAS: String = "certalias"
    }

    lateinit var edit_hostname: EditText
    lateinit var edit_port: EditText
    lateinit var edit_ssl: Switch
    lateinit var edit_clientauth: Switch
    lateinit var field_certalias: EditText
    lateinit var button_select_certAlias: Button
    lateinit var layout_certalias: ViewGroup


    lateinit var button_add: Button
    lateinit var button_import: Button
    lateinit var button_export: Button


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater;

            val dalogView = inflater.inflate(R.layout.modal_add, null)


            // Manual add

            edit_hostname = dalogView.findViewById(R.id.edit_test_hostname)
            edit_port = dalogView.findViewById(R.id.edit_test_port)
            edit_ssl = dalogView.findViewById(R.id.edit_test_ssl)

            button_add = dalogView.findViewById(R.id.button_add)
            button_export = dalogView.findViewById(R.id.button_export)
            button_import = dalogView.findViewById(R.id.button_import)

            edit_clientauth = dalogView.findViewById(R.id.edit_clientauth)
            field_certalias = dalogView.findViewById(R.id.field_test_certalias)
            button_select_certAlias = dalogView.findViewById(R.id.button_select_certalias)
            layout_certalias =  dalogView.findViewById(R.id.layout_certalias)



            // SSL Client Auth

            edit_clientauth.visibility = GONE
            edit_ssl.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked){
                    edit_clientauth.visibility = VISIBLE
                }else{
                    edit_clientauth.visibility = GONE
                    edit_clientauth.isChecked =  false

                }
            }

            layout_certalias.visibility = GONE
            edit_clientauth.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked){
                    layout_certalias.visibility = VISIBLE
                }else{
                    layout_certalias.visibility = GONE
                }
            }
            button_select_certAlias.setOnClickListener {
                KeyChain.choosePrivateKeyAlias(this.requireActivity(),
                    { alias ->
                        if (alias != null) {
                            getActivity()?.runOnUiThread(java.lang.Runnable {
                                field_certalias.setText(alias)
                                Log.i("TEST","alias:selected "+field_certalias.text.toString())

                            })

                        }else{
                            Toast.makeText(context,"No certficate selected", Toast.LENGTH_SHORT)
                        }
                    },  /* keyTypes[] */null,  /* issuers[] */null,  /* uri */null,  /* alias */null)
            }





            // Add
            button_add.setOnClickListener { view ->
                val hostname = edit_hostname.text.toString()
                val port = Integer.parseInt(edit_port.text.toString())
                val ssl = edit_ssl.isChecked

                var bundle = Bundle()
                bundle.putString(KEY_HOSTNAME, hostname)
                bundle.putInt(KEY_PORT, port)
                bundle.putBoolean(KEY_SSL, ssl)
                if(edit_clientauth.isChecked){
                    bundle.putString(KEY_CERTALIAS,field_certalias.text.toString())
                    Log.i("TEST","alias:click "+field_certalias.text.toString())
                }

                var message = Message()
                message.data = bundle
                this.add_handler.dispatchMessage(message)
            }


            // Import
            button_import.setOnClickListener { view ->
                this.import_handler.dispatchMessage(Message())
            }

            button_export.setOnClickListener { view ->
                this.export_handler.dispatchMessage(Message())
            }

            builder.setView(dalogView)
            .setNegativeButton("Close") { dialog, id ->
                    dialog.cancel()
             }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}