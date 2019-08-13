package com.polarstation.diary10.fragment;

public interface FragmentCallBack {
    void findMyDiary();
    void replaceFragment(int type);
    void quitApp();
    void showProgressDialog(String stringResource);
    void cancelDialog();
    void finishActivity();
}
