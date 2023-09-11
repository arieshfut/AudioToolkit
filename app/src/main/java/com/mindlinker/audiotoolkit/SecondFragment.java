package com.mindlinker.audiotoolkit;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mindlinker.audiotoolkit.PreResearch.PreResearchManager;
import com.mindlinker.audiotoolkit.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {
    private static final String TAG = "SecondFragment";

    private boolean isAlsaStart = false;
    private Context context;
    private PreResearchManager preResearch = null;
    private FragmentSecondBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        MainActivity.preMenuOrder = 1;
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = MainActivity.getContext();
        preResearch = PreResearchManager.getInstance();

        binding.buttonAudio8CHPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.showToast("该功能建设中");
            }
        });

        binding.buttonEarpieceMonitor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.showToast("该功能建设中");
            }
        });

        binding.alsaSampleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });

        binding.alsaBitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });

        binding.alsaCardSpinner.setSelection(1);     // 3588 hdmi card
        binding.alsaDeviceSpinner.setSelection(0);     // 3588 hdmi capture
        binding.alsaSampleSpinner.setSelection(1);     // 16000
        binding.alsaChannelSpinner.setSelection(1);     // 2 channel
        binding.alsaBitSpinner.setSelection(0);     // 16 bit

        binding.buttonAlsaCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAlsaStart = !isAlsaStart;
                binding.buttonAlsaCapture.setText(isAlsaStart ? "停止ALSA采集" : "开启ALSA采集");
                MainActivity.showToast("ALSA采集" + (isAlsaStart ? "开始" : "停止"));
                setAlsaParameter();
                preResearch.aslaCapture(isAlsaStart);
            }
        });
    }

    private void setAlsaParameter() {
        int card = binding.alsaCardSpinner.getSelectedItemPosition();
        int device = binding.alsaDeviceSpinner.getSelectedItemPosition();
        int sample = binding.alsaSampleSpinner.getSelectedItemPosition();
        int channel = binding.alsaChannelSpinner.getSelectedItemPosition() + 1;
        int bit = binding.alsaBitSpinner.getSelectedItemPosition();

        switch (sample) {
            case 0:
                sample = 8000;
                break;
            case 2:
                sample = 32000;
                break;
            case 3:
                sample = 44100;
                break;
            case 4:
                sample = 48000;
                break;
            case 5:
                sample = 96000;
                break;
            default:
                sample = 16000;
                break;
        }

        switch (bit) {
            case 1:
                bit = 24;
                break;
            case 2:
                bit = 32;
                break;
            default:
                bit = 16;
                break;
        }

        preResearch.setAlsaParameter(card, device, sample, channel, bit);
    }

    public void releaseSource() {
        if (isAlsaStart) {
            preResearch.aslaCapture(false);
            isAlsaStart = false;
        }

        preResearch = null;
        context = null;
    }

    @Override
    public void onDestroyView() {
        releaseSource();

        super.onDestroyView();
        binding = null;
    }

}