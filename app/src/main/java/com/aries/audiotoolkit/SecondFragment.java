package com.aries.audiotoolkit;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.aries.audiotoolkit.PreResearch.PreResearchManager;
import com.aries.audiotoolkit.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {
    private static final String TAG = "SecondFragment";

    private boolean isAlsaStart = false;
    private boolean isOboeStart = false;
    private Context context;
    private PreResearchManager preResearch = null;
    private FragmentSecondBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化context
        context = MainActivity.getContext();
    }

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

        binding.buttonEarpieceMonitor.setOnClickListener(v -> MainActivity.showToast("该功能建设中"));

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

        binding.oboeAudioApiSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });

        binding.oboeAudioApiSpinner.setSelection(0); // Unspecified系统默认
        binding.oboeSampleRateSpinner.setSelection(1);     // 16K采样率
        binding.oboeChannelCountSpinner.setSelection(0);     // 单声道
        binding.oboeRecordBitSpinner.setSelection(0);     // 16bit
        binding.oboeDeviceRecordSpinner.setSelection(0);     // 默认采集设备跟随系统
        binding.oboeDevicePlaySpinner.setSelection(0);     // 默认播放设备跟随系统

        binding.buttonOboeTest.setOnClickListener(v -> {
            if (binding.oboeRecordSwitch.isChecked() ||
                    binding.oboePlayerSwitch.isChecked()) {

                isOboeStart = !isOboeStart;
                binding.buttonOboeTest.setText(isOboeStart ? "停止测试" : "OBOE测试");
                MainActivity.showToast("OBOE测试" + (isOboeStart ? "开始" : "停止"));
                if (isOboeStart) { setOboeParameter(); }
                preResearch.oboeEnable(isOboeStart);
            } else {
                MainActivity.showToast("请选择录音、播放或者录音播放.");
            }
        });

        binding.alsaCardSpinner.setSelection(1);     // 3588 hdmi card
        binding.alsaDeviceSpinner.setSelection(0);     // 3588 hdmi capture
        binding.alsaSampleSpinner.setSelection(1);     // 16000
        binding.alsaChannelSpinner.setSelection(1);     // 2 channel
        binding.alsaBitSpinner.setSelection(0);     // 16 bit

        binding.buttonAlsaCapture.setOnClickListener(v -> {
            isAlsaStart = !isAlsaStart;
            binding.buttonAlsaCapture.setText(isAlsaStart ? "停止ALSA采集" : "开启ALSA采集");
            MainActivity.showToast("ALSA采集" + (isAlsaStart ? "开始" : "停止"));
            if (isAlsaStart) { setAlsaParameter(); }
            preResearch.aslaCapture(isAlsaStart);
        });
    }

    private void setOboeParameter() {
        int audioApi = binding.oboeAudioApiSpinner.getSelectedItemPosition();
        int recordDeviceId = binding.oboeDeviceRecordSpinner.getSelectedItemPosition();
        int sample = binding.oboeSampleRateSpinner.getSelectedItemPosition();
        int channel = binding.oboeChannelCountSpinner.getSelectedItemPosition();
        int bit = (binding.alsaBitSpinner.getSelectedItemPosition() + 1) * 16;
        int playDeviceId = binding.oboeDevicePlaySpinner.getSelectedItemPosition();

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

        switch (channel) {
            case 0:
                channel = 1;
                break;
            case 2:
                channel = 5;
                break;
            case 3:
                channel = 6;
                break;
            case 4:
                channel = 8;
                break;
            default:
                channel = 2;
                break;
        }
        boolean needRecord = binding.oboeRecordSwitch.isChecked();
        boolean needPlay = binding.oboePlayerSwitch.isChecked();
        preResearch.setOboeParameter(audioApi, needRecord, needPlay, recordDeviceId, sample, channel, bit, playDeviceId);
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