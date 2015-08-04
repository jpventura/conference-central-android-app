package com.udacity.devrel.training.conference.android.service;

public class Xuxu {

    enum Test {
        ZERO, ONE, TWO;

        void execute() {
        }
    }

    public static void execute(int what) {
        if ((what >= 0) && (what < Test.values().length)) {
            Test.values()[what].execute();
        }
    }

}
