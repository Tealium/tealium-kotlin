package com.tealium.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tealium.mobile.R
import com.tealium.viewmodels.ModuleListViewModel

class ModuleListFragment : Fragment() {

    interface Callbacks {
        fun onModuleSelected(moduleName: String)
    }

    private var callbacks: Callbacks? = null
    private lateinit var moduleListRecyclerView: RecyclerView
    private var adapter: ModuleListAdapter? = null

    private val moduleListViewModel: ModuleListViewModel by lazy {
        ViewModelProviders.of(this).get(ModuleListViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_module_list, container, false)
        moduleListRecyclerView = view.findViewById(R.id.module_list_recycler_view)
        moduleListRecyclerView.layoutManager = LinearLayoutManager(context)

        updateUI()

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    private fun updateUI() {
        adapter = ModuleListAdapter(moduleListViewModel.moduleNames)
        moduleListRecyclerView.adapter = adapter
    }

    companion object {
        fun newInstance(): ModuleListFragment {
            return ModuleListFragment()
        }
    }

    private inner class ModuleListViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val moduleNameTextView = itemView.findViewById<TextView>(R.id.module_name_text_view)

        private lateinit var moduleName: String

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(moduleName: String) {
            this.moduleName = moduleName
            moduleNameTextView.text = moduleName
        }

        override fun onClick(v: View?) {
            callbacks?.onModuleSelected(moduleName)
        }
    }

    private inner class ModuleListAdapter(private val moduleNames: List<String>) : RecyclerView.Adapter<ModuleListViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleListViewHolder {
            val view = layoutInflater.inflate(R.layout.list_item_module, parent, false)
            return ModuleListViewHolder(view)
        }

        override fun getItemCount(): Int {
            return moduleNames.count()
        }

        override fun onBindViewHolder(holder: ModuleListViewHolder, position: Int) {
            val moduleName = moduleNames[position]
            holder.bind(moduleName)
        }
    }
}