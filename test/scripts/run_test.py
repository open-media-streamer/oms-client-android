import argparse
import json
import os
import shutil
import subprocess
import sys
import time
import re
from enum import Enum

HOME_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))
TEST_PATH = os.path.join(HOME_PATH, 'test')
DEPS_PATH = os.path.join(HOME_PATH, 'dependencies')

TEST_MODULES = ["':test:util'", "':test:base'", "':test:p2p:util'", "':test:p2p:apiTest'",
                "':test:conference:util'", "':test:conference:apiTest'"]

INST_TESTS = [
    {'module': 'base', 'path': os.path.join(TEST_PATH, 'base'), 'target': 'oms.test.base'},
    {'module': 'conference', 'path': os.path.join(TEST_PATH, 'conference/apiTest'),
     'target': 'oms.test.conference.apitest'},
    {'module': 'p2p', 'path': os.path.join(TEST_PATH, 'p2p/apiTest'),
     'target': 'oms.test.p2p.apitest'}]

UNIT_TESTS = [
    {'module': 'base', 'path': os.path.join(HOME_PATH, 'src/sdk/base'), 'target': 'oms.base'},
    {'module': 'conference', 'path': os.path.join(HOME_PATH, 'src/sdk/conference'),
     'target': 'oms.conference'},
    {'module': 'p2p', 'path': os.path.join(HOME_PATH, 'src/sdk/p2p'), 'target': 'oms.p2p'}]

LOGCAT_SUFFIX = str(int(time.time())) + '.log'


class TestMode(Enum):
    INSTRUMENTATION = 1
    UNIT = 2


def analyse_instrumentation_test_result(result):
    # Return numbers of succeed cases
    ok_num = 0
    with open(result, 'r') as f:
        for line in f:
            if 'OK (1 test)' in line:
                ok_num += 1
    return ok_num


def analyse_unit_test_result(result):
    total_num = 0
    fail_num = 0
    with open(result, 'r') as f:
        for line in f:
            m_failure = re.match('Tests run:\s+(\d+).*?Failures:\s+(\d+)', line)
            m_succeed = re.match('OK.*?(\d+) test', line)
            if m_failure is not None:
                total_num = int(m_failure.group(1))
                fail_num = int(m_failure.group(2))
            if m_succeed is not None:
                total_num = int(m_succeed.group(1))
    return total_num, fail_num


def exec_am_cmd(am_cmd, device, target_package, result_file, logcat_file):
    adb = ['adb'] if device == None else ['adb', '-s', device]
    clean_logcat = ['logcat', '-c']
    subprocess.call(adb + clean_logcat)
    with open(result_file, 'a+') as rf:
        subprocess.call(adb + am_cmd, stdout=rf)
    logcat_cmd = ['logcat', '-d', target_package]
    with open(logcat_file, 'a+') as lf:
        subprocess.call(adb + logcat_cmd, stdout=lf)


def run_cases(mode, module, target_package, log_dir, device, cases=None):
    print '\n> running instrumentation cases on device', device
    if mode == TestMode.INSTRUMENTATION:
        type = 'instrumentation'
    elif mode == TestMode.UNIT:
        type = 'unit'

    result_file = os.path.join(log_dir, module + '-' + type + '-result-' + LOGCAT_SUFFIX)
    logcat_file = os.path.join(log_dir, module + '-' + type + '-logcat-' + LOGCAT_SUFFIX)

    if type == 'instrumentation':
        for case in cases:
            am_cmd = ['shell', 'am', 'instrument', '-w', '-r', '-e', 'debug', 'false', '-e',
                      'class', target_package + '.' + case,
                      target_package + '.test/android.test.InstrumentationTestRunner']
            exec_am_cmd(am_cmd, device, target_package, result_file, logcat_file)
    elif type == 'unit':
        am_cmd = ['shell', 'am', 'instrument', '-w', '-r', '-e', 'debug', 'false',
                  target_package + '.test/android.support.test.runner.AndroidJUnitRunner']
        exec_am_cmd(am_cmd, device, target_package, result_file, logcat_file)

    print '> done.'
    print '  Result file: <LOG_DIR>/' + module + '-' + type + '-result-' + LOGCAT_SUFFIX
    print '  Log file: <LOG_DIR>/' + module + '-' + type + '-logcat-' + LOGCAT_SUFFIX

    return analyse_instrumentation_test_result(
        result_file) if type == 'instrumentation' else analyse_unit_test_result(result_file)


def install_test(test_path, device):
    cmd = [HOME_PATH + '/gradlew', '-q', 'assembleDebug']
    subprocess.call(cmd, cwd=test_path)
    cmd = [HOME_PATH + '/gradlew', '-q', 'assembleDebugAndroidTest']
    subprocess.call(cmd, cwd=test_path)

    cmd = [HOME_PATH + '/gradlew', '-q', 'uninstallAll']
    subprocess.call(cmd, cwd=test_path)
    cmd = [HOME_PATH + '/gradlew', '-q', 'installDebug']
    subprocess.call(cmd, cwd=test_path)
    cmd = [HOME_PATH + '/gradlew', '-q', 'installDebugAndroidTest']
    subprocess.call(cmd, cwd=test_path)
    print '> done.'


def run_instrumentation_test(case_list, log_dir, device):
    # load test cases.
    # [ {'module': '<module>', 'cases': ['case']} ]
    with open(case_list, 'r') as case_file:
        objs = json.loads(case_file.read())

    result = True
    for obj in objs:
        inst_test = [item for item in INST_TESTS if item['module'] == obj['module']][0]
        install_test(inst_test['path'], device)
        succeed = run_cases(TestMode.INSTRUMENTATION, obj['module'], inst_test['target'], log_dir, device, obj['cases'])
        total = len(obj['cases'])
        result = result and (succeed == total)
        print '\n>', obj['module'] + ' result: All:', total, \
            'Succeed:', succeed, 'Failed:', total - succeed
    return result


def run_unit_test(log_dir, device):
    result = True
    for unit_test in UNIT_TESTS:
        prepare_unit_test(unit_test['path'])
        install_test(unit_test['path'], device)
        total, failure = run_cases(TestMode.UNIT, unit_test['module'], unit_test['target'], log_dir,
                                   device)
        succeed = total - failure
        result = result and (succeed == total)
        print '\n>', unit_test['module'] + ' result: All:', total, \
            'Succeed:', succeed, 'Failed:', failure
    return result


def change_config():
    shutil.copyfile(os.path.join(HOME_PATH, 'settings.gradle'),
                    os.path.join(HOME_PATH, 'settings.gradle.bk'))
    modules_included = ''
    for module in TEST_MODULES:
        modules_included += '\ninclude ' + module
    with open(os.path.join(HOME_PATH, 'settings.gradle'), 'a') as settings_file:
        settings_file.write(modules_included)


def recover_config():
    shutil.move(os.path.join(HOME_PATH, 'settings.gradle.bk'),
                os.path.join(HOME_PATH, 'settings.gradle'))


def build_libs(dependencies_dir):
    print '> building sdk libraries...'
    cmd = ['mv', os.path.join(DEPS_PATH, 'libwebrtc'), os.path.join(DEPS_PATH, 'libwebrtc.bk')]
    subprocess.call(cmd)
    shutil.copytree(dependencies_dir, os.path.join(DEPS_PATH, 'libwebrtc'))
    cmd = ['python', HOME_PATH + '/tools/pack.py', '--skip-zip']
    if subprocess.call(cmd):
        sys.exit(1)


def prepare_instrumentation_test():
    print '> copying libs to dependency dirs...'
    testLibs = os.path.join(HOME_PATH, 'test/libs')
    if os.path.exists(testLibs):
        shutil.rmtree(testLibs)
    shutil.copytree(os.path.join(HOME_PATH, 'dist/libs'), testLibs)
    shutil.move(os.path.join(testLibs, 'webrtc/libwebrtc.jar'), testLibs)
    print '> done.'


def prepare_unit_test(unit_path):
    print '> copying libwebrtc dependencies to androidTest'
    jni_path = os.path.join(unit_path, 'src/androidTest/jniLibs')
    if os.path.exists(jni_path):
        shutil.rmtree(jni_path)
    shutil.copytree(os.path.join(DEPS_PATH, 'libwebrtc'), jni_path)
    print '> done.'


def validate_caselist(case_list):
    # check the existence of case list file.
    if not os.path.exists(case_list):
        print 'No case list file found:', case_list
        return False

    # check the format of case list file.
    try:
        with open(case_list, 'r') as case_file:
            json.load(case_file)
    except ValueError as e:
        print 'Failed to load json:', e
        return False
    return True


def recover_deps():
    shutil.rmtree(os.path.join(DEPS_PATH, 'libwebrtc'))
    cmd = ['mv', os.path.join(DEPS_PATH, 'libwebrtc.bk'), os.path.join(DEPS_PATH, 'libwebrtc')]
    subprocess.call(cmd)


def recover_unit_test_environment():
    for unit_test in UNIT_TESTS:
        shutil.rmtree(os.path.join(unit_test['path'], 'src/androidTest/jniLibs'))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Run android instrumentation tests.')
    parser.add_argument('--build-deps', dest='build', action='store_true', default=False,
                        help='Indicates if to build sdk libraries.')
    parser.add_argument('--instrumentation', dest='instrumentation',
                        default=os.path.join(TEST_PATH, 'case_list.json'),
                        help='Run instrumentation test, parameter:location of the case list json file.')
    parser.add_argument('--device', dest='device',
                        help='Id of the android device on which the test will run.'
                             'If there are multiple devices on the test host machine,'
                             'please indicate the device using this parameter.')
    parser.add_argument('--log-dir', dest='log_dir', default=TEST_PATH,
                        help='Location of the directory where logs for this test will output to.')
    parser.add_argument('--dependencies-dir', dest='dependencies_dir', required=True,
                        help='Location of the dependency libraries.')
    parser.add_argument('--unit', dest='unit', action='store_true', default=False,
                        help='Run unit test.')
    args = parser.parse_args()

    result = False
    instrumentation_test = validate_caselist(args.instrumentation)

    if instrumentation_test and not args.unit:
        sys.exit(1)

    # generate sdk libraries.
    if args.build:
        build_libs(args.dependencies_dir)

    if instrumentation_test:
        prepare_instrumentation_test()
        # change settings.gradle to include test modules.
        change_config()
        result = run_instrumentation_test(args.instrumentation, args.log_dir, args.device)
        # recover the settings.gradle
        recover_config()

    if args.unit:
        result = run_unit_test(args.log_dir, args.device) and result
        recover_unit_test_environment()

    # recover deps_path
    recover_deps()

    # collect test results
    sys.exit(not result)
