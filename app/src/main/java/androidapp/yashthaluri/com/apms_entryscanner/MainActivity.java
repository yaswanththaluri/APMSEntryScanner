package androidapp.yashthaluri.com.apms_entryscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private EditText editTextProductId;
    private ImageView  buttonScan;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private int temp = 0, temp2=0;
    LinearLayout sucLay, errLay, scanLay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        sucLay = findViewById(R.id.successLay);
        errLay = findViewById(R.id.alertLay);
        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
        scanLay = findViewById(R.id.scanLay);
        buttonScan = findViewById(R.id.scanInit);
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restartScan();
            }
        });
    }


    private void buttonScan_onClick() {
        temp2=0;
        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        intentIntegrator.setCameraId(0);
        intentIntegrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult != null) {
            String productId = intentResult.getContents();
            scanLay.setVisibility(View.INVISIBLE);
            if (productId.length() == 28)
            {
                errLay.setVisibility(View.INVISIBLE);
                sucLay.setVisibility(View.VISIBLE);
                getDataFromDatabase(productId);
            }
            else
            {
                errLay.setVisibility(View.VISIBLE);
                sucLay.setVisibility(View.INVISIBLE);
                restartScan();
            }
        }
    }

    public void getDataFromDatabase(final String uid)
    {
        databaseReference.child("userInfo").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ProfileHelper profileHelper = dataSnapshot.getValue(ProfileHelper.class);
                String slotNoStr = profileHelper.getSlotNumber();
                Log.i("slott---->", slotNoStr);
                if(temp2==0)
                {
                    if (slotNoStr.equals("None"))
                    {
                        temp2 = 1;
                        searchForEmptySlotAndFillIt(uid);
                    }
                    else
                    {
                        temp2 = 1;
                        updateDepartDetails(slotNoStr, uid);

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void updateDepartDetails(String parkedSlotNo, String userId)
    {
        database.getReference().child("slotsInfo").child("slot"+parkedSlotNo).child("isFilled").setValue("no");
        database.getReference().child("slotsInfo").child("slot"+parkedSlotNo).child("userInSlot").setValue("None");
        database.getReference().child("userInfo").child(userId).child("slotNumber").setValue("None");
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        HistoryHelper helper = new HistoryHelper("Parking-Out", ""+parkedSlotNo, ""+dateFormat.format(date));
        databaseReference.child("UsersHistory").child(userId).push().setValue(helper);
        restartScan();
    }


    public void searchForEmptySlotAndFillIt(final String userId)
    {
        databaseReference.child("slotsInfo").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> slots = dataSnapshot.getChildren().iterator();



                while (slots.hasNext())
                {
                    SlotFetchHelper slotFetchHelper = slots.next().getValue(SlotFetchHelper.class);
                    if (slotFetchHelper.getIsFilled().equals("no") && temp == 0)
                    {
                        temp = 1;
                        int slotNumber = slotFetchHelper.getSlotNumber();
                        database.getReference().child("slotsInfo").child("slot"+slotFetchHelper.getSlotNumber()).child("isFilled").setValue("yes");
                        database.getReference().child("slotsInfo").child("slot"+slotFetchHelper.getSlotNumber()).child("userInSlot").setValue(userId);
                        database.getReference().child("userInfo").child(userId).child("slotNumber").setValue(""+slotNumber);
                        Log.i("Slot", ""+slotFetchHelper.getSlotNumber());
                        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        Date date = new Date();
                        HistoryHelper helper = new HistoryHelper("Parking-in", ""+slotNumber, ""+dateFormat.format(date));
                        databaseReference.child("UsersHistory").child(userId).push().setValue(helper);
                        break;
                    }
                }



            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }

        });
        restartScan();
    }

    public void restartScan()
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                buttonScan_onClick();
            }
        },2000);
    }

}