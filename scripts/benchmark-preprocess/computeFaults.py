import xlrd
import os
import re
OUTPUT_DIRECTORY = "bugs"
def split_ors(s):
    parts = []
    start = 0
    bracket_level = 0
    i = 0
    n = len(s)
    while i < n:
        if s[i] == '(':
            bracket_level += 1
            i += 1
        elif s[i] == ')':
            bracket_level -= 1
            i += 1
        elif i < n-1 and s[i] == '|' and s[i+1] == '|' and bracket_level == 0:
            parts.append(s[start:i])
            start = i + 2
            i += 2
        else:
            i += 1
    parts.append(s[start:])
    return parts

def remove_outer_parentheses(s):
    while s.startswith('(') and s.endswith(')'):
        s = s[1:-1]
    
    # Special handling here for macro definitions starting with IS_ENABLED
    if 'IS_ENABLED' in s:
        s = s.replace('IS_ENABLED', '')
    return s

def parse_expression(expr):
    expr = re.sub(r'\s+', '', expr)
    disjuncts = split_ors(expr)
    result = []
    for disjunct in disjuncts:
        clean_disjunct = remove_outer_parentheses(disjunct)
        conjuncts = clean_disjunct.split('&&')
        cleaned_conjuncts = [remove_outer_parentheses(c) for c in conjuncts]
        result.append(cleaned_conjuncts)
    return result

def process_excel(input_file):
    workbook = xlrd.open_workbook(input_file)
    sheet = workbook.sheet_by_index(0)
    csv_data = {}

    for row_idx in range(0, sheet.nrows):
        try:
            project = sheet.cell_value(row_idx, 0).strip()
            file_rel_path = sheet.cell_value(row_idx, 3).strip()
            expr = sheet.cell_value(row_idx, 4).strip()

            full_path = os.path.join(OUTPUT_DIRECTORY, project, file_rel_path)
            file_dir = os.path.dirname(full_path)
            file_name = os.path.basename(full_path)
            base_name = os.path.splitext(file_name)[0]
            csv_filename = f"{base_name}-fault.csv"
            csv_path = os.path.join(file_dir, csv_filename)

            parsed = parse_expression(expr)
            if csv_path not in csv_data:
                csv_data[csv_path] = []
            csv_data[csv_path].extend(parsed)
        except Exception as e:
            print(f"Error processing row {row_idx+1}: {str(e)}")

    for csv_path, conditions in csv_data.items():
        try:
            os.makedirs(os.path.dirname(csv_path), exist_ok=True)
            with open(csv_path, 'w', newline='') as f:
                for condition in conditions:
                    f.write(','.join(condition) + '\n')
        except Exception as e:
            print(f"Error writing {csv_path}: {str(e)}")

if __name__ == "__main__":
    process_excel("bugs.xls")