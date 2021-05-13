package kr.ac.gachon.sw.gbro.board;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.UploadTask;
import com.google.type.DateTime;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import kr.ac.gachon.sw.gbro.R;
import kr.ac.gachon.sw.gbro.base.BaseActivity;
import kr.ac.gachon.sw.gbro.databinding.ActivityWriteBinding;
import kr.ac.gachon.sw.gbro.util.Auth;
import kr.ac.gachon.sw.gbro.util.CloudStorage;
import kr.ac.gachon.sw.gbro.util.Firestore;
import kr.ac.gachon.sw.gbro.util.LoadingDialog;
import kr.ac.gachon.sw.gbro.util.Util;

public class WriteActivity extends BaseActivity<ActivityWriteBinding> implements AddImageAdapter.OnImageAddItemClickListener {
    private ActionBar actionBar;
    private AddImageAdapter addImageAdapter;
    private RecyclerView addImageRecyclerView;
    private LoadingDialog loadingDialog;

    @Override
    protected ActivityWriteBinding getBinding() {
        return ActivityWriteBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.write);
        }

        loadingDialog = new LoadingDialog(this);

        // 날짜 및 시간 관련 설정
        setDateTime();

        // 이미지 추가 RecyclerView 설정
        setAddImageRecyclerView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.writemenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Google 정책에 따라 MenuItem에 Switch 사용하지 않고 if문 사용
        int itemId = item.getItemId();

        // 저장 버튼
        if (itemId == R.id.write_save) {
            savePost();
            return true;
        }
        else if(itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        closeDialog();
    }

    /**
     * 데이터가 날아갈 수 있다는 경고 Dialog를 출력한다 - 예를 누르면 Finish
     * @author Minjae Seon
     */
    private void closeDialog() {

        if(!binding.etContent.getText().toString().isEmpty() || !binding.etTitle.getText().toString().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.warning))
                    .setMessage(getString(R.string.post_cancel_dialog_msg))
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(getString(R.string.no), null)
                    .create().show();
        }
        else {
            finish();
        }
    }

    /**
     * 날짜 및 시간 관련 설정
     * @author Minjae Seon
     */
    private void setDateTime() {
        // Calendar Instance
        Calendar cal = Calendar.getInstance();

        // Date & Time Format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        SimpleDateFormat timeFormat = new SimpleDateFormat("a hh:mm", Locale.KOREA);

        // Date & Time EditText
        EditText etDate = binding.etMissingdate;
        etDate.setText(dateFormat.format(cal.getTime()));

        EditText etTime = binding.etMissingtime;
        etTime.setText(timeFormat.format(cal.getTime()));

        // DatePicker
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                Util.debugLog(WriteActivity.this, "onDateSet - " + year + "-" + month + "-" + dayOfMonth);
                cal.set(year, month, dayOfMonth);
                etDate.setText(dateFormat.format(cal.getTime()));
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));

        // Set Max Date
        datePickerDialog.getDatePicker().setMaxDate(cal.getTimeInMillis());

        // TimePicker
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Calendar currentCal = Calendar.getInstance();
                Calendar checkCal = Calendar.getInstance();
                checkCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                checkCal.set(Calendar.MINUTE, minute);

                Util.debugLog(WriteActivity.this, "currentCal - " + currentCal.get(Calendar.YEAR) + "-" + (currentCal.get(Calendar.MONTH) + 1) + "-" + currentCal.get(Calendar.DAY_OF_MONTH) + " "
                        + currentCal.get(Calendar.HOUR_OF_DAY) + ":" + currentCal.get(Calendar.MINUTE) + " / " + currentCal.getTimeInMillis() );
                Util.debugLog(WriteActivity.this, "checkCal - " + checkCal.get(Calendar.YEAR) + "-" + (checkCal.get(Calendar.MONTH) + 1) + "-" + checkCal.get(Calendar.DAY_OF_MONTH) + " "
                        + checkCal.get(Calendar.HOUR_OF_DAY) + ":" + checkCal.get(Calendar.MINUTE) + " / " + checkCal.getTimeInMillis());

                // 현재 시간보다 미래가 아니면
                if(currentCal.getTimeInMillis() >= checkCal.getTimeInMillis()) {
                    // 설정
                    cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), hourOfDay, minute);
                    etTime.setText(timeFormat.format(cal.getTime()));
                }
                else {
                    // 미래면 토스트 출력
                    Toast.makeText(WriteActivity.this, getString(R.string.post_futureerror), Toast.LENGTH_SHORT).show();

                    view.setHour(currentCal.get(Calendar.HOUR_OF_DAY));
                    view.setMinute(currentCal.get(Calendar.MINUTE));
                }
            }
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false);

        binding.etMissingdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerDialog.show();
            }
        });

        binding.etMissingtime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timePickerDialog.show();

            }
        });
    }

    private void savePost(){
        // 제목과 내용이 비어있지 않으면
        if(!binding.etTitle.getText().toString().replaceAll("\\s", "").isEmpty() && !binding.etContent.getText().toString().replaceAll("\\s", "").isEmpty()){
            // 사진이 1장 이상 있다면
            if(addImageAdapter.getAllImageList().size() > 1) {
                // 작성 Task
                new WritePostTask().execute();
            }
            else {
                Toast.makeText(this, R.string.post_nophoto, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(),R.string.post_empty,Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Image 추가를 위한 RecyclerView를 설정한다
     * @author Minjae Seon
     */
    private void setAddImageRecyclerView() {
        Util.debugLog(this, "setAddImageRecyclerView()");
        addImageAdapter = new AddImageAdapter(this);
        addImageRecyclerView = binding.rvPhoto;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        addImageRecyclerView.setHasFixedSize(true);
        addImageRecyclerView.setLayoutManager(linearLayoutManager);
        addImageRecyclerView.setAdapter(addImageAdapter);
    }

    @Override
    public void onAddClick(View v) {
        ImagePicker.Companion.with(this)
                .crop()
                .galleryMimeTypes(new String[]{"image/png", "image/jpg", "image/jpeg"})
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start();
    }

    @Override
    public void onRemoveClick(View v, int position) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.warning))
                .setMessage(getString(R.string.post_deletephoto_msg))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addImageAdapter.removeImage(position);
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK) {
            Bitmap fileBitmap = BitmapFactory.decodeFile(ImagePicker.Companion.getFilePath(data));
            addImageAdapter.addImage(fileBitmap);
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
        }
    }

    private class WritePostTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected void onPreExecute() {
            loadingDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<Bitmap> allImageList = addImageAdapter.getAllImageList();

            // 포스트 작성
            Firestore.writeNewPost(binding.spinnerPosttype.getSelectedItemPosition() + 1,
                    binding.etTitle.getText().toString(),
                    binding.etContent.getText().toString(),
                    allImageList.size()-1,
                    binding.spinnerBuilding.getSelectedItemPosition(),
                    new ArrayList<GeoPoint>(),
                    Auth.getCurrentUser().getUid(),
                    new Timestamp(new Date()), false)
                    .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentReference> task) {
                            if(task.isSuccessful()) {
                                // 사진 업로드
                                for(int i = 1; i < allImageList.size(); i++) {
                                    int currentNum = i;
                                    CloudStorage.uploadPostImg(task.getResult().getId(), String.valueOf(i), allImageList.get(i))
                                            .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                                    if(task.isSuccessful()) {
                                                        Util.debugLog(WriteActivity.this, "Photo #" + String.valueOf(currentNum) + " Upload Success");
                                                    }
                                                    else {
                                                        Util.debugLog(WriteActivity.this, "Photo #" + String.valueOf(currentNum) + " Upload Failed!");
                                                    }

                                                    if(currentNum == allImageList.size() - 1) loadingDialog.dismiss(); finish();
                                                }
                                            });
                                }
                            }
                            else {
                                Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            return null;
        }
    }
}
