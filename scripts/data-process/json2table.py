import os
import json
import csv
from configuration import *

def read_json(json_data, key):
    if key not in KEY_LISTS:
        raise ValueError(f"Invalid key: {key}")
    value = json_data.get(key, '-')
    if value is None or value == '':
        value = '-'
    
    if key == 'time' and type(value) == int:
        value = f"{value / 1000000:.3f}"
    
    return value

def process_json_files(directory, key):
    key_results = {sut: {} for sut in SUTS}
    
    for filename in os.listdir(directory):
        if filename.endswith('.json'):
            try:
                method, sut = filename[:-5].split('_')
                if sut not in SUTS or method not in METHODS:
                    continue
            except:
                continue
            
            filepath = os.path.join(directory, filename)
            try:
                with open(filepath, 'r') as f:
                    data = json.load(f)
                value = read_json(data, key)
                key_results[sut][f'{method}'] = value 
            except (json.JSONDecodeError, IOError):
                key_results[sut][f'{method}'] = '-'
    
    return key_results

def write_to_csv(results, filename):
    headers = ['SUT']
    for method in METHODS:
        headers.append(f'{method}')

    with open(filename, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(headers)
        
        for sut in SUTS:
            row = [sut]
            for method in METHODS:
                content = results[sut].get(f'{method}')
                if content == None:
                    content = '-'
                
                row.append(content)
            writer.writerow(row)

if __name__ == '__main__':
    
    for i in range(REPEAT_NUM):
        process_dir = os.path.join(RESULTS_DIR, str(i))
        

        if not os.path.exists(process_dir):
            print(f"{process_dir} not exists, skip it.")
            continue
        print(f"processing {process_dir}")

        for key in KEY_LISTS:
            results = process_json_files(process_dir, key)
            output_path = os.path.join(process_dir, f'{key}.csv')
            write_to_csv(results, output_path)
            print(f"generate csv file: {output_path}")
