import os
import csv

def process_directory(input_dir):
    for root, dirs, files in os.walk(input_dir):
        for file in files:
            if file.endswith(('.c', '.h')):
                source_path = os.path.join(root, file)
                base = os.path.splitext(source_path)[0]
                model_csv = base + '-model.csv'
                fault_csv = base + '-fault.csv'
                
                if not os.path.exists(model_csv):
                    print(f"Error: Missing model CSV for {source_path}")
                    continue
                if not os.path.exists(fault_csv):
                    print(f"Error: Missing fault_csv CSV for {source_path}")
                    continue
                
                param_dict = {}
                try:
                    with open(model_csv, 'r') as f:
                        reader = csv.reader(f)
                        next(reader)
                        for row in reader:
                            if len(row) < 2:
                                continue
                            param_num = int(row[0])
                            param_name = row[1].strip()
                            param_dict[param_name] = param_num
                except Exception as e:
                    print(f"Error reading model CSV {model_csv}: {e}")
                    continue
                
                param_numbers = sorted(param_dict.values())
                if not param_numbers:
                    print(f"No parameters found in {model_csv}")
                    continue
                expected_numbers = list(range(len(param_numbers)))
                if param_numbers != expected_numbers:
                    print(f"Error: Parameters in {model_csv} have non-consecutive or non-zero-starting numbers: {param_numbers}")
                    continue
                num_params = len(param_numbers)
                
                mfs_csv = base + '-mfs.csv'
                try:
                    with open(fault_csv, 'r') as mfs_file, open(mfs_csv, 'w', newline='') as fault_file:
                        mfs_reader = csv.reader(mfs_file)
                        fault_writer = csv.writer(fault_file)
                        for mfs_row in mfs_reader:
                            fault_row = ['-'] * num_params
                            for element in mfs_row:
                                element = element.strip()
                                if not element:
                                    continue
                                if element.startswith('!'):
                                    param_name = element[1:]
                                    value = '0'
                                else:
                                    param_name = element
                                    value = '1'
                                if param_name not in param_dict:
                                    print(f"Warning: Parameter '{param_name}' in {fault_csv} not found in model. Skipping.")
                                    continue
                                param_num = param_dict[param_name]
                                fault_row[param_num] = value
                            fault_writer.writerow(fault_row)
                except Exception as e:
                    print(f"Error processing fault CSV {fault_csv}: {e}")
                    continue

def main():
    process_directory('bugs')

if __name__ == '__main__':
    main()