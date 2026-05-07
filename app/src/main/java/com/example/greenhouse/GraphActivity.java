package com.example.greenhouse;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class GraphActivity extends AppCompatActivity {

    private TextView btnMinggu, btnBulan;
    private LineChartView lineChartSuhu, lineChartKelembapan;
    private LinearLayout labelsSuhu, labelsKelembapan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_graph);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnMinggu = findViewById(R.id.btnMinggu);
        btnBulan = findViewById(R.id.btnBulan);
        lineChartSuhu = findViewById(R.id.lineChartSuhu);
        lineChartKelembapan = findViewById(R.id.lineChartKelembapan);
        labelsSuhu = findViewById(R.id.labelsSuhu);
        labelsKelembapan = findViewById(R.id.labelsKelembapan);

        btnMinggu.setOnClickListener(v -> selectMinggu());
        btnBulan.setOnClickListener(v -> selectBulan());

        // Default: Minggu
        selectMinggu();

        // Bottom Nav logic
        findViewById(R.id.navProfil).setOnClickListener(v -> finish());
    }

    private void selectMinggu() {
        btnMinggu.setBackgroundResource(R.drawable.bg_filter_selected);
        btnMinggu.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        btnBulan.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnBulan.setTextColor(ContextCompat.getColor(this, R.color.green_secondary));

        updateCharts("minggu");
    }

    private void selectBulan() {
        btnBulan.setBackgroundResource(R.drawable.bg_filter_selected);
        btnBulan.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        btnMinggu.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnMinggu.setTextColor(ContextCompat.getColor(this, R.color.green_secondary));

        updateCharts("bulan");
    }

    private void updateCharts(String filter) {
        labelsSuhu.removeAllViews();
        labelsKelembapan.removeAllViews();

        if (filter.equals("minggu")) {
            String[] days = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
            double[] values = {67.21, 36.52, 67.11, 48.68, 68.5, 94.26, 62.55};

            lineChartSuhu.setData(values);
            lineChartKelembapan.setData(values);

            for (String day : days) {
                addLabel(labelsSuhu, day);
                addLabel(labelsKelembapan, day);
            }
        } else {
            String[] dates = {"1", "5", "10", "15", "20", "25", "30"};
            double[] values = new double[dates.length];
            for (int i = 0; i < dates.length; i++) {
                values[i] = Math.random() * 80 + 20;
                addLabel(labelsSuhu, dates[i]);
                addLabel(labelsKelembapan, dates[i]);
            }
            lineChartSuhu.setData(values);
            lineChartKelembapan.setData(values);
        }
    }

    private void addLabel(LinearLayout container, String label) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(10);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(params);
        container.addView(tv);
    }
}