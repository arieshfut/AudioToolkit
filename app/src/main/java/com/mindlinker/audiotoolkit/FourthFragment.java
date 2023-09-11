package com.mindlinker.audiotoolkit;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mindlinker.audiotoolkit.databinding.FragmentFourthBinding;


public class FourthFragment extends Fragment {
    private static final String TAG = "FourthFragment";

    private Context context;
    private FragmentFourthBinding binding;

    // mrtc auto test
    private boolean isStarting = false;
    private Button mAudioTestButton = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = MainActivity.getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        MainActivity.preMenuOrder = 3;
        View rootView = inflater.inflate(R.layout.toolkit_about, container, false);

        return rootView;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void releaseSource() {
        context = null;
    }

    @Override
    public void onDestroyView() {
        releaseSource();

        super.onDestroyView();
        binding = null;
    }
}