package com.tealium.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tealium.mobile.R
import com.tealium.mobile.TealiumHelper
import com.tealium.mobile.databinding.FragmentMomentsApiBinding
import com.tealium.momentsapi.EngineResponse
import com.tealium.momentsapi.ErrorCode
import com.tealium.momentsapi.ResponseListener
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MomentsApiFragment : Fragment() {
    private lateinit var binding: FragmentMomentsApiBinding
    private lateinit var momentsAttrListRecyclerView: RecyclerView
    private var adapter: MomentsAttrListAdapter? = null

    private var lastEngineId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMomentsApiBinding.inflate(inflater, container, false)
        momentsAttrListRecyclerView = binding.momentsAttrList
        momentsAttrListRecyclerView.layoutManager = LinearLayoutManager(context)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)

        val savedEngineId = sharedPreferences.getString("engineId", "")
        savedEngineId?.let {
            lastEngineId = it
            binding.editEngineId.setText(it)
        }

        binding.editEngineId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                lastEngineId = s.toString()
                sharedPreferences.edit().putString("engineId", lastEngineId).apply()
            }
        })

        binding.fetchEngineResponseButton.setOnClickListener {
            TealiumHelper.trackEvent("fetch engine button click", emptyMap())
            Executors.newSingleThreadScheduledExecutor().schedule({
                onFetchEngineData()
            }, 550, TimeUnit.MILLISECONDS)
        }
    }

    private fun onFetchEngineData() {
        TealiumHelper.getMomentsVisitorData(
            lastEngineId,
            object : ResponseListener<EngineResponse> {
                override fun success(data: EngineResponse) {
                    activity?.runOnUiThread {
                        parseAndUpdateUI(data)
                    }
                }

                override fun failure(errorCode: ErrorCode, message: String) {
                    activity?.runOnUiThread {
                        createToast("Engine Response Error: ${errorCode.value} - $message")
                    }
                }
            })
    }

    @UiThread
    private fun parseAndUpdateUI(engineResponse: EngineResponse) {
        val responseList = mutableListOf<MomentsDataEntry>()
        engineResponse.audiences?.let {
            responseList.add(MomentsDataEntry("Audiences", it.toString()))
        }

        engineResponse.badges?.let {
            responseList.add(MomentsDataEntry("Badges", it.toString()))
        }

        engineResponse.strings?.let {
            responseList.add(MomentsDataEntry("Strings", it.toString()))
        }

        engineResponse.numbers?.let {
            responseList.add(MomentsDataEntry("Numbers", it.toString()))
        }

        engineResponse.booleans?.let {
            responseList.add(MomentsDataEntry("Booleans", it.toString()))
        }

        engineResponse.dates?.let {
            responseList.add(MomentsDataEntry("Dates", it.toString()))
        }

        adapter = MomentsAttrListAdapter(responseList.toList())
        momentsAttrListRecyclerView.adapter = adapter
    }

    @UiThread
    private fun createToast(message: String) {
        Toast.makeText(this@MomentsApiFragment.context, message, Toast.LENGTH_SHORT)
            .show()
    }

    private inner class MomentsAttrListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val momentsLabelTextView = itemView.findViewById<TextView>(R.id.moments_attr_name_label)
        val momementsPlaceholderTextView =
            itemView.findViewById<TextView>(R.id.moments_attr_text_placeholder)

        fun bind(label: String, text: String) {
            momentsLabelTextView.text = label
            momementsPlaceholderTextView.text = text
        }
    }

    private inner class MomentsAttrListAdapter(private val momentsData: List<MomentsDataEntry>) :
        RecyclerView.Adapter<MomentsAttrListViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MomentsAttrListViewHolder {
            val view = layoutInflater.inflate(R.layout.moments_attr_list_item, parent, false)
            return MomentsAttrListViewHolder(view)
        }

        override fun getItemCount(): Int {
            return momentsData.count()
        }

        override fun onBindViewHolder(holder: MomentsAttrListViewHolder, position: Int) {
            val entry = momentsData[position]
            holder.bind(entry.label, entry.text)
        }
    }
}

data class MomentsDataEntry(val label: String, val text: String)