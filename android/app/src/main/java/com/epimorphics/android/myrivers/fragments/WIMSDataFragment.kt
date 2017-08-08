package com.epimorphics.android.myrivers.fragments

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TextView
import com.epimorphics.android.myrivers.R
import com.epimorphics.android.myrivers.activities.DataViewActivity
import com.epimorphics.android.myrivers.data.Measurement
import com.epimorphics.android.myrivers.data.WIMSPoint
import com.epimorphics.android.myrivers.helpers.FragmentHelper


/**
 * Created by mihajlo on 07/07/17.
 */

open class WIMSDataFragment : FragmentHelper() {
    private lateinit var mToolbar: Toolbar  // The toolbar.
    private lateinit var mBackButton: ImageButton
    private lateinit var mMoreInfoButton: Button
    private lateinit var mWIMSDataView: View
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mWIMSDataFragment: WIMSDataFragment
    private lateinit var mDataViewActivity: DataViewActivity
    private lateinit var mMeasurementTable: TableLayout

    private val TAG = "WIMS_DATA_FRAGMENT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        mDataViewActivity = activity as DataViewActivity
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_wims_data_view, container, false)
        mWIMSDataView = view
        mWIMSDataFragment = this

        // Initialise Recycler View and hide it
        mRecyclerView = view.bind(R.id.wims_recycler_view)
        mRecyclerView.visibility = View.INVISIBLE

        mMeasurementTable = view.bind(R.id.wims_table)

        val wimsPoint = mDataViewActivity.selectedWIMSPoint
//        WIMSPointRatingsAPI(mWIMSDataFragment).execute(wimsPoint)
//        WIMSPointMetalsAPI(mWIMSDataFragment).execute(wimsPoint)

        mToolbar = view.bind(R.id.wims_data_toolbar)

        mBackButton = view.bind(R.id.back_button_wims_data_view)
        mBackButton.setOnClickListener { activity.onBackPressed() }

        mMoreInfoButton = view.bind(R.id.info_button_wims_data_view)
        mMoreInfoButton.setOnClickListener {
            mDataViewActivity.openWIMSDetailsFragment()
        }
        mMoreInfoButton.visibility = View.INVISIBLE

        return view
    }

    fun showMoreInfoButton() {
        mMoreInfoButton.visibility = View.VISIBLE
    }

    fun setMeasurementsText(wimsPoint: WIMSPoint) {
//        Log.i(TAG, "Number of measurements pulled: " + wimsPoint.measurementMap.size )
        val wimsName: TextView = mWIMSDataView.bind(R.id.wims_name)
        wimsName.text = if(wimsPoint.label != null) "Samples from ${wimsPoint.label}" else "Samples from ${wimsPoint.id}"

        var rowIndex = 0
        var tableHeaderRow = newTableRow(rowIndex++)

        val hasGeneralRecords = wimsPoint.measurementMap.any {
            entry: Map.Entry<String, ArrayList<Measurement>> ->
                WIMSPoint.generalGroup.contains(entry.key)
        }

        if (hasGeneralRecords) {
            addTextView(tableHeaderRow, "Determinand", 0.28, R.style.text_view_table_parent, Gravity.START)
            addTextView(tableHeaderRow, "Sample Dates", 0.72, R.style.text_view_table_parent)

            mMeasurementTable.addView(tableHeaderRow)

            for (entry in WIMSPoint.generalGroup) {
                if (wimsPoint.measurementMap.containsKey(entry) && wimsPoint.measurementMap[entry]!!.isNotEmpty()) {
                    val measurementList = arrayListOf<Measurement>()
                    for (measure in wimsPoint.measurementMap[entry]!!) {
                        measurementList.add(measure)
                    }
                    if (measurementList.size > 2) {
                        tableHeaderRow = newTableRow(rowIndex++, true, 1)

                        val descriptor = measurementList[0].descriptor
                        addTextView(tableHeaderRow, entry, 0.28, R.style.text_view_table_parent_light, Gravity.START, 0, descriptor)
                        addTextView(tableHeaderRow, simplifyDate(measurementList[0].date), 0.24, R.style.text_view_table_parent_light, Gravity.END)
                        addTextView(tableHeaderRow, simplifyDate(measurementList[1].date), 0.24, R.style.text_view_table_parent_light, Gravity.END)
                        addTextView(tableHeaderRow, simplifyDate(measurementList[2].date), 0.24, R.style.text_view_table_parent_light, Gravity.END)

                        mMeasurementTable.addView(tableHeaderRow)

                        tableHeaderRow = newTableRow(rowIndex++, true, 1)

                        val unit = measurementList[0].unit
                        addTextView(tableHeaderRow, "($unit)", 0.28, R.style.text_view_table_child, Gravity.START)
                        addTextView(tableHeaderRow, measurementList[0].result.toString(), 0.24, R.style.text_view_table_child, Gravity.END)
                        addTextView(tableHeaderRow, measurementList[1].result.toString(), 0.24, R.style.text_view_table_child, Gravity.END)
                        addTextView(tableHeaderRow, measurementList[2].result.toString(), 0.24, R.style.text_view_table_child, Gravity.END)

                        mMeasurementTable.addView(tableHeaderRow)
                    }
                }
            }
        } else {
            val noDataExplanation: TextView = mWIMSDataView.bind(R.id.wims_no_data)
            noDataExplanation.visibility = View.VISIBLE
        }
    }
}