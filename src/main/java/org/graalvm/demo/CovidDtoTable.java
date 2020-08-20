package org.graalvm.demo;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;

public final class CovidDtoTable {
	
	public DepartmentProxyArrayColumn dep;
	public CsvFilePathProxyArrayColumn csvFilePath;
	

	public CovidDtoTable (CovidDto[] dto) {
		this.dep= new DepartmentProxyArrayColumn(dto);
		this.csvFilePath= new CsvFilePathProxyArrayColumn(dto);
		
	}
	
	
	public static final class CovidDto {
		
		public String dep;
		public String csvFilePath;
		public CovidDto( String dep,String csvFilePath) {
			this.dep=dep;
			this.csvFilePath=csvFilePath;
		}

	}
	
	
	public static class DepartmentProxyArrayColumn implements ProxyArray {
        private final CovidDto[] dto;

        public DepartmentProxyArrayColumn(CovidDto[] dto) {
            this.dto = dto;
        }

        public Object get(long index) {
            return dto[(int) index].dep;
        }

        public void set(long index, Value value) {
            throw new UnsupportedOperationException();
        }

        public long getSize() {
            return dto.length;
        }
    }

	
	public static class CsvFilePathProxyArrayColumn implements ProxyArray {
        private final CovidDto[] dto;

        public CsvFilePathProxyArrayColumn(CovidDto[] dto) {
            this.dto = dto;
        }

        public Object get(long index) {
            return dto[(int) index].csvFilePath;
        }

        public void set(long index, Value value) {
            throw new UnsupportedOperationException();
        }

        public long getSize() {
            return dto.length;
        }
    }

	
}