package com.nosari20.connectivitytest.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Switch
import androidx.fragment.app.DialogFragment
import com.nosari20.connectivitytest.R
import kotlinx.android.synthetic.main.modal_add.*

class AddTestDialogFragment(val handler: Handler) : DialogFragment() {

    companion object {
        val KEY_HOSTNAME: String = "hostname"
        val KEY_PORT: String = "port"
        val KEY_SSL: String = "ssl"
    }

    lateinit var edit_hostname: EditText
    lateinit var edit_port: EditText
    lateinit var edit_ssl: Switch

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater;

            val dalogView = inflater.inflate(R.layout.modal_add, null)

            edit_hostname = dalogView.findViewById(R.id.edit_test_hostname)
            edit_port = dalogView.findViewById(R.id.edit_test_port)
            edit_ssl = dalogView.findViewById(R.id.edit_test_ssl)

            builder.setView(dalogView)
                .setPositiveButton("OK",
                    DialogInterface.OnClickListener { dialog, id ->

                        val hostname = edit_hostname.text.toString()
                        val port = Integer.parseInt(edit_port.text.toString())
                        val ssl = edit_ssl.isChecked

                        var bundle = Bundle()
                        bundle.putString(AddTestDialogFragment.KEY_HOSTNAME, hostname)
                        bundle.putInt(AddTestDialogFragment.KEY_PORT, port)
                        bundle.putBoolean(AddTestDialogFragment.KEY_SSL, ssl)

                        var message = Message()
                        message.data = bundle
                        this.handler.dispatchMessage(message)
                    })

                .setNegativeButton("Cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        dialog.cancel()
                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}