package com.polarstation.diary10.activity;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import gun0912.tedkeyboardobserver.TedKeyboardObserver;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.ActivityEditAccountBinding;
import com.polarstation.diary10.util.NetworkStatus;

import java.util.HashMap;
import java.util.Map;

import static com.polarstation.diary10.util.NetworkStatus.TYPE_CONNECTED;

public class EditAccountActivity extends BaseActivity implements View.OnClickListener{
    private ActivityEditAccountBinding binding;
    private FirebaseStorage strInstance;
    private FirebaseDatabase dbInstance;
    private String uid;
    private String imageUrl = "";
    private Uri imageUri;
    private int netStat;

    public static final String NAME_KEY = "nameKey";
    public static final String COMMENT_KEY = "commentKey";
    public static final String URL_KEY = "urlKey";
    public static final String URI_KEY = "uriKey";
    public static final int PICK_FROM_ALBUM_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_account);

        Intent intent = getIntent();
        if(intent != null) {
            String urlFromAccount =  intent.getStringExtra(URL_KEY);
            String name = intent.getStringExtra(NAME_KEY);
            String comment = intent.getStringExtra(COMMENT_KEY);

            Glide.with(getBaseContext())
                    .load(urlFromAccount)
                    .apply(new RequestOptions().circleCrop())
                    .into(binding.editAccountActivityProfileImageView);
            binding.editAccountActivityNameEditText.setText(name);
            binding.editAccountActivityCommentEditText.setText(comment);
        }

        strInstance = FirebaseStorage.getInstance();
        dbInstance = FirebaseDatabase.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        binding.editAccountActivityProfileImageView.setOnClickListener(this);
        binding.editAccountActivityCloseButton.setOnClickListener(this);
        binding.editAccountActivitySaveButton.setOnClickListener(this);

        // 키보드 올라오거나 내려갈때 이벤트
        new TedKeyboardObserver(this).listen(isShow-> {
            if(!isShow && binding.editAccountActivityNameEditText.isFocused())
                binding.editAccountActivityNameEditText.clearFocus();
            else if(!isShow && binding.editAccountActivityCommentEditText.isFocused())
                binding.editAccountActivityCommentEditText.clearFocus();
        });
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.editAccountActivity_closeButton:
                finish();
                break;
            case R.id.editAccountActivity_saveButton:
                netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
                if(netStat == TYPE_CONNECTED) {
                    String name = String.valueOf(binding.editAccountActivityNameEditText.getText());
                    if (name.equals(""))
                        Toast.makeText(this, getString(R.string.write_name), Toast.LENGTH_SHORT).show();
                    else {
                        Intent resultIntent = new Intent();
                        String comment = String.valueOf(binding.editAccountActivityCommentEditText.getText());

                        Map<String, Object> map = new HashMap<>();
                        map.put(getString(R.string.fdb_user_name), name);
                        dbInstance.getReference().child(getString(R.string.fdb_users)).child(uid).updateChildren(map);
                        map.clear();
                        map.put(getString(R.string.fdb_comment), comment);
                        dbInstance.getReference().child(getString(R.string.fdb_users)).child(uid).updateChildren(map);

                        resultIntent.putExtra(NAME_KEY, name);
                        resultIntent.putExtra(COMMENT_KEY, comment);

                        if (imageUri != null)
                            updateProfileImage(imageUri, resultIntent);
                        else {
                            binding.editAccountActivityProgressBar.setVisibility(View.INVISIBLE);
                            resultIntent.putExtra(URI_KEY, "");
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        }
                    }
                }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
                break;
            case R.id.editAccountActivity_profileImageView:
                Intent requestIntent = new Intent(Intent.ACTION_PICK);
                requestIntent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(requestIntent, PICK_FROM_ALBUM_CODE);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == PICK_FROM_ALBUM_CODE && resultCode == Activity.RESULT_OK){
            imageUri = data.getData();

            Glide.with(getBaseContext())
                    .load(imageUri)
                    .apply(new RequestOptions().circleCrop())
                    .into(binding.editAccountActivityProfileImageView);
        }
    }

    private void setViewWhenUploading(){
        binding.editAccountActivityProgressLayout.setVisibility(View.VISIBLE);
        binding.editAccountActivityConstraintLayout.setEnabled(false);
        binding.editAccountActivityCloseButton.setEnabled(false);
        binding.editAccountActivitySaveButton.setEnabled(false);
        binding.editAccountActivityProfileImageView.setEnabled(false);
        binding.editAccountActivityCommentEditText.setEnabled(false);
        binding.editAccountActivityNameEditText.setEnabled(false);
    }

    private void updateProfileImage(Uri imageUri, Intent resultIntent){
        setViewWhenUploading();
        strInstance.getReference().child(getString(R.string.fstr_user_images)).child(uid).putFile(imageUri)
                .addOnCompleteListener(task -> {
                    strInstance.getReference().child(getString(R.string.fstr_user_images)).child(uid).getDownloadUrl().addOnSuccessListener(uri -> {
                        imageUrl = String.valueOf(uri);
                        resultIntent.putExtra(URI_KEY, String.valueOf(imageUrl));

                        Map<String, Object> stringObjectMap = new HashMap<>();
                        stringObjectMap.put(getString(R.string.fdb_profile_image_url), imageUrl);
                        dbInstance.getReference().child(getString(R.string.fdb_users)).child(uid).updateChildren(stringObjectMap)
                        .addOnSuccessListener( aVoid -> {
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        });
                    });

                });
    }
}
