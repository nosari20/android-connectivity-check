package com.nosari20.connectivitytest.ui.dialog

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.app.Instrumentation
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.DialogFragment
import com.nosari20.connectivitytest.R

import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat


class AddTestDialogFragment(val add_handler: Handler,val import_handler: Handler, val export_handler: Handler) : DialogFragment() {

    companion object {
        val KEY_HOSTNAME: String = "hostname"
        val KEY_PORT: String = "port"
        val KEY_SSL: String = "ssl"
    }

    lateinit var edit_hostname: EditText
    lateinit var edit_port: EditText
    lateinit var edit_ssl: Switch


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


            button_add.setOnClickListener { view ->
                val hostname = edit_hostname.text.toString()
                val port = Integer.parseInt(edit_port.text.toString())
                val ssl = edit_ssl.isChecked

                var bundle = Bundle()
                bundle.putString(KEY_HOSTNAME, hostname)
                bundle.putInt(KEY_PORT, port)
                bundle.putBoolean(KEY_SSL, ssl)

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
            .setNegativeButton("Cancel") { dialog, id ->
                    dialog.cancel()
             }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}