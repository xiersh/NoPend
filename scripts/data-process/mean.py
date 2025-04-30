from configuration import *
import os
import csv
import numpy as np

def compute_average_for_csv(directory, filename):
    print(filename)
    headers = METHODS
    first_column = SUTS
    
    subdirs = [d for d in os.listdir(directory) if d.isdigit()]
    subdirs.sort(key=int)
    
    if not subdirs:
        print(f"Error: Do not find digit subdirectories in {directory}. ")
        return
    
    # Initialize data structure
    all_data = []
    
    for subdir in subdirs:
        filepath = os.path.join(directory, subdir, filename)
        if not os.path.exists(filepath):
            print(f"Warning: {filepath} does not exist, skipping")
            continue
            
        with open(filepath, 'r') as f:
            reader = csv.reader(f)
            data = list(reader)
            
            # Check if file format is correct
            if len(data) != len(first_column) + 1:  # header + data rows
                print(f"Warning: {filepath} has unexpected number of rows, skipping")
                continue
                
            if data[0][1:] != headers:
                print(f"Warning: {filepath} has unexpected headers, skipping")
                continue
                
            # Extract data (keep '-', convert others to float)
            current_data = []
            for row in data[1:]:
                current_row = []
                for cell in row[1:]:
                    if cell.strip() == '-':
                        current_row.append('-')
                    else:
                        try:
                            current_row.append(float(cell))
                        except ValueError:
                            current_row.append('-')  # Treat as timeout/out-of-memory if not a number
                current_data.append(current_row)
                
            all_data.append(current_data)
    
    if not all_data:
        print("No available data files for averaging")
        return
    
    # Calculate final results
    final_data = []
    for row_idx in range(len(first_column)):
        final_row = []
        for col_idx in range(len(headers)):
            # Check if this cell has '-' in any run
            has_timeout = any(
                isinstance(all_data[run_idx][row_idx][col_idx], str) and 
                all_data[run_idx][row_idx][col_idx] == '-'
                for run_idx in range(len(all_data))
            )
            
            if has_timeout:
                final_row.append('-')
            else:
                # Collect all non-'-' values
                values = [
                    all_data[run_idx][row_idx][col_idx]
                    for run_idx in range(len(all_data))
                    if not (isinstance(all_data[run_idx][row_idx][col_idx], str) and 
                           all_data[run_idx][row_idx][col_idx] == '-')
                ]
                if values:
                    if filename == "pendingSchemaProportion.csv" or filename == "precision.csv" or filename == "recall.csv":
                        avg_value = round(np.mean(values), 5)
                    else:
                        avg_value = round(np.mean(values), 3)
                    final_row.append(avg_value)
                else:
                    final_row.append('-')
        final_data.append(final_row)
    
    # Prepare output data
    output_data = []
    # Add header
    output_data.append(["SUT"] + headers)
    # Add data rows 
    for i, row in enumerate(final_data):
        output_data.append([first_column[i]] + row)
    
    output_path = os.path.join(directory, filename)
    with open(output_path, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerows(output_data)
    
    print(f"Results saved to {output_path}")

if __name__ == "__main__":
    for key in KEY_LISTS:
        compute_average_for_csv(RESULTS_DIR, f"{key}.csv")
        