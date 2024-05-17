# Performance test for reactive vs regular concurrency model

Make sure you have sdk 17 active in the current shell

on line 36 in the docker file set TEST_SERVER to either http://client:8080/greet or TEST_SERVER: http://reactive-client:8080/greet

run the performance test with `sh run-tests.sh` which builds the neccessary images

Change the code 