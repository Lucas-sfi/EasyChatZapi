package com.example.easychat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ProfileFragment extends Fragment {

    ImageView profilePic;
    EditText usernameInput;
    EditText phoneInput;
    EditText ageInput;
    EditText cityInput;
    EditText emailInput;
    EditText newPasswordInput;
    EditText confirmPasswordInput;
    Button updateProfileBtn;
    ProgressBar progressBar;
    TextView logoutBtn;
    RadioGroup statusRadioGroup;
    RadioButton radioOnline, radioOffline, radioBusy;

    UserModel currentUserModel;
    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;

    public ProfileFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data!=null && data.getData()!=null){
                            selectedImageUri = data.getData();
                            AndroidUtil.setProfilePic(getContext(),selectedImageUri,profilePic);
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_profile, container, false);
        profilePic = view.findViewById(R.id.profile_image_view);
        usernameInput = view.findViewById(R.id.profile_username);
        phoneInput = view.findViewById(R.id.profile_phone);
        ageInput = view.findViewById(R.id.profile_age);
        cityInput = view.findViewById(R.id.profile_city);
        emailInput = view.findViewById(R.id.profile_email);
        newPasswordInput = view.findViewById(R.id.profile_new_password);
        confirmPasswordInput = view.findViewById(R.id.profile_confirm_password);
        updateProfileBtn = view.findViewById(R.id.profle_update_btn);
        progressBar = view.findViewById(R.id.profile_progress_bar);
        logoutBtn = view.findViewById(R.id.logout_btn);
        statusRadioGroup = view.findViewById(R.id.status_radio_group);
        radioOnline = view.findViewById(R.id.radio_online);
        radioOffline = view.findViewById(R.id.radio_offline);
        radioBusy = view.findViewById(R.id.radio_busy);

        getUserData();

        updateProfileBtn.setOnClickListener((v -> updateBtnClick()));

        logoutBtn.setOnClickListener((v)->{
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    FirebaseUtil.logout();
                    Intent intent = new Intent(getContext(),SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            });
        });

        profilePic.setOnClickListener((v)->{
            ImagePicker.with(this).cropSquare().compress(512).maxResultSize(512,512)
                    .createIntent(intent -> {
                        imagePickLauncher.launch(intent);
                        return null;
                    });
        });

        statusRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (currentUserModel != null) {
                String status = "offline";
                if (checkedId == R.id.radio_online) {
                    status = "online";
                } else if (checkedId == R.id.radio_busy) {
                    status = "busy";
                }
                currentUserModel.setUserStatus(status);
                updateToFirestore();
            }
        });

        return view;
    }

    void updateBtnClick(){
        String newUsername = usernameInput.getText().toString();
        String newAgeStr = ageInput.getText().toString();
        String newCity = cityInput.getText().toString();
        String newEmail = emailInput.getText().toString();
        String newPassword = newPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if(newUsername.isEmpty() || newUsername.length()<3){
            usernameInput.setError("Username length should be at least 3 chars");
            return;
        }

        currentUserModel.setUsername(newUsername);
        currentUserModel.setSearchUsername(newUsername.toLowerCase());
        currentUserModel.setCity(newCity);
        if (!newAgeStr.isEmpty()) {
            currentUserModel.setAge(Integer.parseInt(newAgeStr));
        } else {
            currentUserModel.setAge(0);
        }

        setInProgress(true);

        if (currentUserModel.getEmail() == null && !newEmail.isEmpty()) {
            handleLinkEmail(newEmail, newPassword, confirmPassword);
        } else if (currentUserModel.getEmail() != null && !newPassword.isEmpty()) {
            handleUpdatePassword(newPassword, confirmPassword);
        } else {
            if(selectedImageUri!=null){
                FirebaseUtil.getCurrentProfilePicStorageRef().putFile(selectedImageUri)
                        .addOnCompleteListener(task -> updateToFirestore());
            }else{
                updateToFirestore();
            }
        }
    }

    void handleLinkEmail(String email, String password, String confirmPassword) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("E-mail inválido");
            setInProgress(false);
            return;
        }
        if (password.length() < 6) {
            newPasswordInput.setError("A senha deve ter pelo menos 6 caracteres");
            setInProgress(false);
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("As senhas não coincidem");
            setInProgress(false);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(email, password);
            user.linkWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            currentUserModel.setEmail(email);
                            if(selectedImageUri!=null){
                                FirebaseUtil.getCurrentProfilePicStorageRef().putFile(selectedImageUri)
                                        .addOnCompleteListener(imageTask -> updateToFirestore());
                            }else{
                                updateToFirestore();
                            }
                        } else {
                            Toast.makeText(getContext(), "Falha ao vincular e-mail: " + task.getException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            setInProgress(false);
                        }
                    });
        } else {
            setInProgress(false);
        }
    }

    void handleUpdatePassword(String password, String confirmPassword) {
        if (password.length() < 6) {
            newPasswordInput.setError("A senha deve ter pelo menos 6 caracteres");
            setInProgress(false);
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("As senhas não coincidem");
            setInProgress(false);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.updatePassword(password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Senha atualizada com sucesso", Toast.LENGTH_SHORT).show();
                    if(selectedImageUri!=null){
                        FirebaseUtil.getCurrentProfilePicStorageRef().putFile(selectedImageUri)
                                .addOnCompleteListener(imageTask -> updateToFirestore());
                    }else{
                        updateToFirestore();
                    }
                } else {
                    Toast.makeText(getContext(), "Falha ao atualizar senha: " + task.getException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    setInProgress(false);
                }
            });
        } else {
            setInProgress(false);
        }
    }

    void updateToFirestore(){
        FirebaseUtil.currentUserDetails().set(currentUserModel)
                .addOnCompleteListener(task -> {
                    setInProgress(false);
                    if(task.isSuccessful()){
                        AndroidUtil.showToast(getContext(),"Updated successfully");
                    }else{
                        AndroidUtil.showToast(getContext(),"Updated failed");
                    }
                });
    }

    void getUserData(){
        setInProgress(true);
        FirebaseUtil.getCurrentProfilePicStorageRef().getDownloadUrl()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        Uri uri  = task.getResult();
                        AndroidUtil.setProfilePic(getContext(),uri,profilePic);
                    }
                });

        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            setInProgress(false);
            currentUserModel = task.getResult().toObject(UserModel.class);
            if(currentUserModel != null){
                usernameInput.setText(currentUserModel.getUsername());
                phoneInput.setText(currentUserModel.getPhone());
                cityInput.setText(currentUserModel.getCity());
                if (currentUserModel.getAge() > 0) {
                    ageInput.setText(String.valueOf(currentUserModel.getAge()));
                }

                if (currentUserModel.getEmail() != null && !currentUserModel.getEmail().isEmpty()) {
                    emailInput.setText(currentUserModel.getEmail());
                    emailInput.setEnabled(false);
                } else {
                    emailInput.setEnabled(true);
                }

                switch (currentUserModel.getUserStatus()) {
                    case "online":
                        radioOnline.setChecked(true);
                        break;
                    case "busy":
                        radioBusy.setChecked(true);
                        break;
                    default:
                        radioOffline.setChecked(true);
                        break;
                }
            }
        });
    }

    void setInProgress(boolean inProgress){
        if(inProgress){
            progressBar.setVisibility(View.VISIBLE);
            updateProfileBtn.setVisibility(View.GONE);
        }else{
            progressBar.setVisibility(View.GONE);
            updateProfileBtn.setVisibility(View.VISIBLE);
        }
    }
}