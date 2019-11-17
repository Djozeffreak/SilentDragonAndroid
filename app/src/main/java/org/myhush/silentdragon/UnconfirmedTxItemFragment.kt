package org.myhush.silentdragon

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.beust.klaxon.Klaxon
import java.text.DecimalFormat

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [UnconfirmedTxItemFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [UnconfirmedTxItemFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class UnconfirmedTxItemFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var tx: DataModel.TransactionItem? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tx = Klaxon().parse(it.getString(ARG_PARAM1))
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_unconfirmed_tx_item, container, false)

        view.findViewById<ConstraintLayout>(R.id.layoutUnconfirmedItem).setOnClickListener { v ->
            val intent = Intent(activity, TxDetailsActivity::class.java)
            intent.putExtra("EXTRA_TXDETAILS", Klaxon().toJsonString(tx))
            startActivity(intent)
        }

        val txt = view.findViewById<TextView>(R.id.txtUnconfirmedTx)
        txt.text = (if (tx?.type == "send") "Sending" else "Receiving") +
                    DecimalFormat("#0.00########").format(kotlin.math.abs(tx?.amount?.toDoubleOrNull() ?: 0.0)) +  " ${DataModel.mainResponseData?.tokenName} "

        return view
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment UnconfirmedTxItemFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            UnconfirmedTxItemFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
