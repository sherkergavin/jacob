# JACOB

Small [CBOR][1] implementation for Java, based on the proposed standard of the
CBOR specification, RFC 7049.

## Supported types

All major types as defined by CBOR are supported.

## Supported values

All basic Java types are supported, including support for half-precision float values 
as defined in IEEE 754. Note that currently not the all values of an `uint64_t` value 
are supported by this implementation.

## To do

- [x] add support for encoding/decoding semantic tags;
- [ ] add better support for decoding unlimited arrays, strings and maps;
- [ ] implement support for `uint64_t` types that cannot handled directly by Java's long value;
- [ ] better error handling and testing.

## License

Apache Public License v2.0

## Author

J.W. Janssen <j.w.janssen@lxtreme.nl>

## References

[1]. http://tools.ietf.org/html/rfc7049
