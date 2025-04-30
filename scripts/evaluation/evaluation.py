import subprocess
import json
import os
import csv
from pathlib import Path
import time
from scripts.configuration import *

# replace with your own resources directory
RESOURCES_DIR = r"path/to/resources"
TIMEOUT_SECONDS = 600 # 10 minutes
# replace with your own classpath
JAVA_CLASSPATH = "path/to/classpath"
JAVA_OPTS = "-Xmx1G"  # 1GB memory limit
BENCHMARK_PATH = os.path.join(RESOURCES_DIR, "benchmark.csv")  # CSV file path

def parse_benchmark_csv(csv_file):
    """Parse benchmark.csv file and return a mapping from subject_path to max_mfs"""
    subject_info = {}
    
    with open(csv_file, mode='r', encoding='utf-8') as file:
        reader = csv.DictReader(file)
        
        for row in reader:
            subject_path = row['SUT']
            size_of_mfs = row['Size of MFS']
            
            # Calculate max_mfs (take the maximum value from Size of MFS)
            if ';' in size_of_mfs:
                max_mfs = max(int(x) for x in size_of_mfs.split(';'))
            else:
                max_mfs = int(size_of_mfs)
            
            subject_info[subject_path] = max_mfs
    
    return subject_info

def build_java_command(method, subject_path, max_mfs):
    absolute_subject_path = os.path.join(RESOURCES_DIR, subject_path)
    return [
        r"path/to/java", # replace with your own java path
        JAVA_OPTS,
        "-cp", JAVA_CLASSPATH,
        "nju.gist.Main",
        absolute_subject_path, 
        method, 
        str(max_mfs)
    ]

def run_experiment(method, subject_path, max_mfs):
    """Run a single experiment"""
    SUT_name = SUBJECTS_DICT[subject_path]
    output_file = Path(output_dir) / f"{method}_{SUT_name}.json"
    
    cmd = build_java_command(method, subject_path, max_mfs)
    
    result = None
    try:
        # Run Java program and capture output
        result = subprocess.run(cmd, check=True, capture_output=True, text=True, timeout=TIMEOUT_SECONDS)
        
        # Parse JSON output
        result_data = json.loads(result.stdout)
        
        # Add metadata
        result_data.update({
            "start time": time.strftime("%Y-%m-%d %H:%M:%S", time.localtime()),
            "method": method,
            "subject": subject_path,
            "max_mfs": max_mfs,
            "status": "success", 
            "stdout": result.stdout,
            "stderr": result.stderr,
            "timeout limit": str(TIMEOUT_SECONDS) + "s", 
            "memory limit": "1G"
        })
        
        # Save results
        with open(output_file, "w") as f:
            json.dump(result_data, f, indent=2)
            
        print(f"✅ Success: {method} on {subject_path}")

    except subprocess.TimeoutExpired:
        error_data = {
            "status": "timeout",
            "method": method,
            "subject": subject_path,
            "timeout": True
        }
        print(f"❌ Failed: {method} on {subject_path} - timeout after {TIMEOUT_SECONDS} seconds")
        with open(output_file, "w") as f:
            json.dump(error_data, f, indent=2)

        
    except subprocess.CalledProcessError as e:
        error_data = {
            "method": method,
            "subject": subject_path,
            "max_mfs": max_mfs,
            "status": "error",
            "error": e.stderr
        }
        with open(output_file, "w") as f:
            json.dump(error_data, f, indent=2)
        print(f"❌ Failed: {method} on {subject_path} - {e.stderr}")
    
    except json.JSONDecodeError:
        error_data = {
            "method": method,
            "subject": subject_path,
            "max_mfs": max_mfs,
            "status": "error",
            "error": "Invalid JSON output", 
            "stdout": result.stdout if result else "No output"
        }
        with open(output_file, "w") as f:
            json.dump(error_data, f, indent=2)
        print(f"❌ Failed: {method} on {subject_path} - Invalid JSON output")

def main_serial():
    subject_info = parse_benchmark_csv(BENCHMARK_PATH)
    if not subject_info:
        print("Error: No subjects found in benchmark.csv")
        return
    print(f"Loaded {len(subject_info)} subjects from benchmark.csv")
    
    for method in METHODS:
        for subject_path, max_mfs in subject_info.items():
            run_experiment(method, subject_path, max_mfs)

if __name__ == "__main__":
    for i in range(REPEAT_NUM):
        output_dir = os.path.join(RESULTS_DIR, str(i))
        if os.path.exists(output_dir):
            print(f"Directory {output_dir} already exists. Skipping.")
            continue
        Path(output_dir).mkdir(exist_ok=True)
        main_serial()