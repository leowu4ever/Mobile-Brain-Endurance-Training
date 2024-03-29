package com.kent.lw.brainendurancetrainingmobileapp;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class TrainingFragment extends Fragment {

    private TrainingCommunicator trainingCommunicator;

    private Button btnPause, btnFinish;

    private ImageButton btnLock;
    private TextView tvDuration;
    private TextView tvDistance, tvPace, tvSpeed, tvCurSpeed;
    private TextView tvStiCount, tvHitCount, tvLapseCount, tvResCount, tvAccuracy, tvAvgResTime;
    private TextView tvLogRes, tvLogSti;
    private DialogHelper dh;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_training, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initUIs();
    }

    private void initUIs() {
        trainingCommunicator = (TrainingCommunicator) getActivity();
        dh = new DialogHelper(getActivity());
        btnPause = getActivity().findViewById(R.id.btn_pause);

        btnFinish = getActivity().findViewById(R.id.btn_finish);
        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trainingCommunicator.finishTraining();

            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trainingCommunicator.pauseTraining();
            }
        });

        tvDuration = getActivity().findViewById(R.id.tv_duration);
        tvDistance = getActivity().findViewById(R.id.tv_distance);
        tvSpeed = getActivity().findViewById(R.id.tv_speed);
        tvCurSpeed = getActivity().findViewById(R.id.tv_cur_speed);
        tvPace = getActivity().findViewById(R.id.tv_pace);
        tvStiCount = getActivity().findViewById(R.id.tv_sti_count);
        tvLapseCount = getActivity().findViewById(R.id.tv_lapse_count);
        tvHitCount = getActivity().findViewById(R.id.tv_hit_count);
        tvResCount = getActivity().findViewById(R.id.tv_res_count);
        tvAccuracy = getActivity().findViewById(R.id.tv_accuracy);
        tvAvgResTime = getActivity().findViewById(R.id.tv_avg_res_time);

        tvLogRes = getActivity().findViewById(R.id.tv_log_res);
        tvLogSti = getActivity().findViewById(R.id.tv_log_sti);

        btnLock = getActivity().findViewById(R.id.btn_lock);
        btnLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dh.showLockDialog();
            }
        });
    }

    public void setTvDuration(String s) {
        tvDuration.setText(s);
    }

    public void setTvDistance(String s) {
        tvDistance.setText(s);
    }

    public void setTvSpeed(String s) {
        tvSpeed.setText(s);
    }

    public void setTvPace(String s) {
        tvPace.setText(s);
    }

    public void setTvStiCount(String s) {
        tvStiCount.setText(s);
    }

    public void setTvHitCount(String s) {
        tvHitCount.setText(s);
    }

    public void setTvAccuracy(String s) {
        tvAccuracy.setText(s);
    }

    public void setTvAvgResTime(String s) {
        tvAvgResTime.setText(s);
    }

    public void setTvLogRes(String s) {
        tvLogRes.setText(s);
    }

    public void setTvSti(String s) {
        tvLogSti.setText(s);
    }

    public void setTvLapseCount(String s) {
        tvLapseCount.setText(s);
    }

    public void setTvResCount(String s) {
        tvResCount.setText(s);
    }

    public void setTvCurSpeed(String s) {
        tvCurSpeed.setText(s);
    }
}
