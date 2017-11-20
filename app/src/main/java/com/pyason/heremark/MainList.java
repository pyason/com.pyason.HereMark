package com.pyason.heremark;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by paulyason on 2015-07-21.
 */
public class MainList extends ListFragment implements AdapterView.OnItemClickListener {

    private int stateInt;
    private final String FRAGMENT_KEY = "saved_fragment";
    ArrayList<String> list = new ArrayList<>();
    ArrayAdapter<String> mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.list_fragment, container, false);

        /*ListView view = (ListView) v.findViewById(android.R.id.list);
        view.setEmptyView(v.findViewById(android.R.id.empty));

        return view;*/
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRetainInstance(true);

        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, list);

        setListAdapter(mAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(parent.getContext(), "Item Pressed!", Toast.LENGTH_LONG).show();
    }

}
